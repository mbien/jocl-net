/*
 * Created on 15. October 2007, 14:38
 */
package com.mbien.opencl.net.shoal;

import java.util.logging.Logger;
import com.mbien.opencl.net.CLNetwork;
import com.mbien.opencl.net.LocalNode;
import com.mbien.opencl.net.remote.RemoteNode;
import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GMSFactory;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.MessageSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.enterprise.ee.cms.impl.client.FailureNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.JoinedAndReadyNotificationActionFactoryImpl;
import com.sun.enterprise.ee.cms.impl.client.MessageActionFactoryImpl;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static java.util.logging.Level.*;

/**
 * Controller for a single GMS grid node.
 * @author Michael Bien
 */
public class GMSGridNodeController extends CLNetwork {

    private static final String IP_KEY = "MEMBER_IP";
    private static final Logger LOGGER = Logger.getLogger(GMSGridNodeController.class.getName());

    private GroupManagementService gms;
    private LocalNode localNode;

    private final List<RemoteNode> nodes;
    private final String group;

    public GMSGridNodeController(String group) {
        this.nodes = new ArrayList<RemoteNode>();
        this.group = group;
    }

    @Override
    public void startNode(String name) {

        if(localNode != null) {
            throw new RuntimeException("node is already running");
        }

        localNode = new LocalNode(group, name);

        try {

            gms = (GroupManagementService)GMSFactory.startGMSModule(localNode.name, localNode.group, MemberType.CORE, null);

//            gms.addActionFactory(new JoinNotificationActionFactoryImpl(this));
//            gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(logging()));
            gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(onNodeJoin()));
            gms.addActionFactory(new FailureNotificationActionFactoryImpl(onNodeQuit()));
//            gms.addActionFactory(new FailureSuspectedActionFactoryImpl(logging()));
//            gms.addActionFactory(new FailureNotificationActionFactoryImpl(logging()));
//            gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
            gms.addActionFactory(new MessageActionFactoryImpl(ipReply()), "discovery");

            LOGGER.info("Starting Node...\n");
            gms.join();
            gms.updateMemberDetails(localNode.name, IP_KEY, getIP4String());

            List<String> members = gms.getGroupHandle().getAllCurrentMembers();
            for (String member : members) {
                addNode(member, (String)gms.getMemberDetails(member).get(IP_KEY));
            }

            LOGGER.info("Node running\n");
        } catch (GMSException ex) {
            throw new RuntimeException("Cannot initialize GMS", ex);
        }

    }

    @Override
    public void shutdownNode() {
        LOGGER.info("Shutting down");
        gms.shutdown(GMSConstants.shutdownType.INSTANCE_SHUTDOWN);
        LOGGER.info("done");
    }

    private CallBack ipReply() {
        return new SignalAdapter<MessageSignal>() {
            @Override public void onMessage(MessageSignal msg) {
                try {
                    String addresses = getIP4String();

                    gms.getGroupHandle().sendMessage(msg.getMemberToken(), addresses.getBytes());
                } catch (GMSException ex) {
                    LOGGER.log(SEVERE, "can not reply", ex);
                }
            }

        };
    }

    private CallBack onNodeJoin() {
        return new SignalAdapter<JoinedAndReadyNotificationSignal>() {
            @Override public void onMessage(JoinedAndReadyNotificationSignal msg) {
                String name = msg.getMemberToken();
                addNode(name, (String)msg.getMemberDetails().get(IP_KEY));
            }

        };
    }

    private CallBack onNodeQuit() {
        return new SignalAdapter<FailureNotificationSignal>() {
            @Override public void onMessage(FailureNotificationSignal msg) {
                String name = msg.getMemberToken();
                removeNode(name);
            }

        };
    }

    private void addNode(String name, String ips) {

        if(this.localNode.name.equals(name)) {
            return;
        }

        try {
            String[] parts = ips.split("|");
            System.out.println("ip string: "+ips);
            InetAddress address = InetAddress.getByName(ips);
            System.out.println("picking: "+address);
            RemoteNode gridNode = new RemoteNode(localNode.group, name, address);
            synchronized(nodes) {
                nodes.add(gridNode);
            }
        } catch (UnknownHostException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void removeNode(String name) {

        synchronized(nodes) {
            Iterator<RemoteNode> iterator = nodes.iterator();
            while(iterator.hasNext()) {
                if(iterator.next().name.equals(name)) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    private CallBack logging() {
        return new SignalAdapter<Signal>() {
            @Override public void onMessage(Signal signal) {
                if (LOGGER.isLoggable(INFO)) {
                    LOGGER.info("Received " + signal.getClass().getName()
                            + "\n - Source Member: " + signal.getMemberToken()
                            + "\n - Source Group: " + signal.getGroupName());
                }
            }
        };
    }

    private String getIP4String() {
        Set<InetAddress> ips = getOwnIP4s();
        Iterator<InetAddress> iterator = ips.iterator();
        String addresses = iterator.next().getHostAddress();
        while(iterator.hasNext()) {
            addresses += "|" + iterator.next().getHostAddress();
        }
        return addresses;
    }

    /**
     * Returns all own IPs.
     */
    private Set<InetAddress> getOwnIP4s() {
        try {
            Set<InetAddress> addresses = new HashSet<InetAddress>();

            Enumeration ifaces = NetworkInterface.getNetworkInterfaces();

            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) ifaces.nextElement();

                Enumeration addrs = ni.getInetAddresses();

                while (addrs.hasMoreElements()) {
                    InetAddress ia = (InetAddress) addrs.nextElement();
                    if(ia instanceof Inet4Address && !ia.isLoopbackAddress()) {
                        addresses.add(ia);
                    }
                }
            }

            return addresses;
        } catch (SocketException ex) {
            throw new RuntimeException("can not retrieve ip adresses", ex);
        }

    }

    @Override
    public LocalNode getLocalNode() {
        return localNode;
    }

    @Override
    public List<RemoteNode> getRemoteNodes() {
        synchronized(nodes){
            return Collections.unmodifiableList(nodes);
        }
    }


    private abstract class SignalAdapter <T extends Signal> implements CallBack {

        @Override
        public void processNotification(Signal signal) {
            try {
                signal.acquire();
                onMessage((T)signal);
                signal.release();
            } catch (SignalReleaseException ex) {
                LOGGER.log(WARNING, "Exception occured while acquiring signal", ex);
            } catch (SignalAcquireException ex) {
                LOGGER.log(WARNING, "Exception occured while releasing signal", ex);
            }
        }

        public abstract void onMessage(T signal);
    }

}