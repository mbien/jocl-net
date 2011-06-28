/*
 * Created on Saturday, June 10 2011 17:09
 */
package com.mbien.opencl.net.demo;

import com.jogamp.opencl.CLCommandQueue;
import com.jogamp.opencl.CLContext;
import com.jogamp.opencl.CLDevice.Type;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.util.CLMultiContext;
import com.jogamp.opencl.util.CLPlatformFilters;
import com.jogamp.opencl.util.Filter;
import com.jogamp.opencl.util.concurrent.CLCommandQueuePool;
import com.jogamp.opencl.util.concurrent.CLQueueContext;
import com.jogamp.opencl.util.concurrent.CLQueueContextFactory;
import com.mbien.opencl.net.CLNetwork;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 *
 * @author Michael Bien
 */
public class SHA2Breaker {

    private static CLPlatform[] getPlatfoms() {
        return CLPlatform.listCLPlatforms(
                new Filter<CLPlatform>() {
                    @Override public boolean accept(CLPlatform item) {
                        return !item.getName().contains("Intel");
                    }
                },
                CLPlatformFilters.type(Type.ALL)
        );
    }

    private static List<CLPlatform> getNWPlatfoms() throws InterruptedException {
        CLNetwork nw = CLNetwork.createNetwork("jocl-net");
        nw.startNode("foo");

        Thread.sleep(15000);
        return nw.getPlatforms(
                new Filter<CLPlatform>() {
                    @Override public boolean accept(CLPlatform item) {
                        return !item.getName().contains("Intel");
                    }
                },
                CLPlatformFilters.type(Type.CPU)
        );
    }



    public static void main(String[] args) throws NoSuchAlgorithmException, InterruptedException, ExecutionException {


//        final String pw = "bdbkgc";
        final String pw = "Agc*";
        final byte[] hash = MessageDigest.getInstance("SHA-512").digest(pw.getBytes());


        CLMultiContext mc = CLMultiContext.create(getPlatfoms());
//        CLMultiContext mc = CLMultiContext.create(getNWPlatfoms());

        for (CLContext ctx : mc.getContexts()) {
            System.out.println(ctx);
        }

        try{

            CLQueueContextFactory<SHA2BreakerContext> factory = new CLQueueContextFactory<SHA2BreakerContext>() {
                @Override public SHA2BreakerContext setup(CLCommandQueue queue, CLQueueContext old) {
                    return new SHA2BreakerContext(queue, hash, pw.length(), ' ', 95);
                }
            };

//            String pass = new SHA2BreakerTask().execute(factory.setup(mc.getContexts().get(0).getMaxFlopsDevice().createCommandQueue(), null));
//            System.out.println(pass);

            CLCommandQueuePool pool = CLCommandQueuePool.create(factory, mc);

            List<SHA2BreakerTask> tasks = new ArrayList<SHA2BreakerTask>();
            for (int i = 0; i < pool.getSize(); i++) {
                tasks.add(new SHA2BreakerTask());
            }

            String password = (String) pool.invokeAny(tasks);
            System.out.println(password);

            for (SHA2BreakerTask task : tasks) {
                System.out.println(task.getDeviceName()+" made "+task.getIterations()+" iterations");
            }
            
//            synchronized(SHA2Breaker.class) {
//                SHA2Breaker.class.wait();
//            }
            
        }finally{
//            mc.release();
//            nw.shutdownNode();
        }
    }

    public static String bytesToHex(byte[] b) {
        String s = "";
        for (int i = 0; i < b.length; i++) {
            if (i > 0 && i % 4 == 0) {
                s += " ";
            }
            s += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return s;
    }

    public static byte[] hexToBytes(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            b[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return b;
    }

    //... not finished
    private static void readShadow() {
//        sudo useradd foo
//        sudo passwd abc
//        or
//        mkpasswd -m sha-512 abc CPGbAXV.
        String shadow = "foo:$6$CPGbAXV.$FbApbt5nRVxp7bh9iwC4XEN7eG.M.Rtu3Ezl4u1mzZNPzdI2bsuulVFS94x17Nt2qD8/SNte7r9RvxOu458aD1:14739:0:99999:7:::";
        String[] fields = shadow.split(":");

        String user = fields[0];
        String pw = fields[1];
        System.out.println(pw);

        String[] parts = pw.split("\\$");
        String id = parts[1];
        String salt = parts[2];
        String hash = parts[3];

        System.out.println(user);
        System.out.println(id);
        System.out.println(salt);
        System.out.println(hash);
        
    }

}
