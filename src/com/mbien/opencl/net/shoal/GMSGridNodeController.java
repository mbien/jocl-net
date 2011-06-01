/*
 * Created on 15. October 2007, 14:38
 */
package com.mbien.opencl.net.shoal;

import com.sun.enterprise.ee.cms.core.GroupHandle;
import java.util.logging.Logger;
import com.mbien.opencl.net.CLNetwork;
import com.mbien.opencl.net.LocalNode;
import com.mbien.opencl.net.remote.RemoteNode;
import com.sun.enterprise.ee.cms.core.CallBack;
import com.sun.enterprise.ee.cms.core.GMSConstants;
import com.sun.enterprise.ee.cms.core.GMSException;
import com.sun.enterprise.ee.cms.core.GMSFactory;
import com.sun.enterprise.ee.cms.core.GroupManagementService;
import com.sun.enterprise.ee.cms.core.GroupManagementService.MemberType;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    private final String group;

    public GMSGridNodeController(String group) {
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
//            gms.addActionFactory(new JoinedAndReadyNotificationActionFactoryImpl(onNodeJoin()));
//            gms.addActionFactory(new FailureNotificationActionFactoryImpl(onNodeQuit()));
//            gms.addActionFactory(new FailureSuspectedActionFactoryImpl(logging()));
//            gms.addActionFactory(new FailureNotificationActionFactoryImpl(logging()));
//            gms.addActionFactory(new PlannedShutdownActionFactoryImpl(this));
//            gms.addActionFactory(new MessageActionFactoryImpl(ipReply()), "discovery");

            LOGGER.info("Starting Node...\n");
            gms.join();
            gms.updateMemberDetails(localNode.name, IP_KEY, getIP4String());

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

    private RemoteNode createNode(String name, String ips) {

        try {
            String[] parts = ips.split("|");
            System.out.println("ip string: "+ips);
            InetAddress address = InetAddress.getByName(ips);
            System.out.println("picking: "+address);

            return new RemoteNode(localNode.group, name, address);
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
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

    private GroupHandle getGroup() {
        return gms.getGroupHandle();
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
        List<String> members = getGroup().getAllCurrentMembers();
        List<RemoteNode> nodes = new ArrayList<RemoteNode>(members.size());
        for (String name : members) {
            if(!getLocalNode().name.equals(name)) {
                String ips = (String) gms.getMemberDetails(name).get(IP_KEY);
                nodes.add(createNode(name, ips));
            }
        }
        return nodes;
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