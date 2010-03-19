package udt.util;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import udt.UDTClient;
import udt.UDTInputStream;
import udt.UDTOutputStream;

/**
 * helper class for receiving a single file via UDT
 * Intended to be compatible with the C++ version in 
 * the UDT reference implementation
 * 
 * main method USAGE: 
 * java -cp ... udt.util.ReceiveFile <server_ip> <server_port> <remote_filename> <local_filename>
 */
public class ReceiveFile implements Runnable{

	private final int serverPort;
	private final String serverHost;
	private final String remoteFile;
	private final String localFile;
	
	public ReceiveFile(String serverHost, int serverPort, String remoteFile, String localFile){
		this.serverHost=serverHost;
		this.serverPort=serverPort;
		this.remoteFile=remoteFile;
		this.localFile=localFile;
	}
	
	public void run(){
		try{
			UDTClient client=new UDTClient(InetAddress.getByName("localhost"),64738);
			client.connect(serverHost, serverPort);
			UDTInputStream in=client.getInputStream();
			UDTOutputStream out=client.getOutputStream();
			
			byte[]readBuf=new byte[1024];
			ByteBuffer bb=ByteBuffer.wrap(readBuf);
			
			//send name file info
			byte[]fName=remoteFile.getBytes();
			bb.putInt(fName.length);
			bb.put(fName);
			
			out.write(readBuf, 0, bb.position());
			
			//read size info (an 4-byte int) 
			byte[]sizeInfo=new byte[4];
			in.read(sizeInfo);
			long size=ByteBuffer.wrap(sizeInfo).getInt();
			
			File file=new File(new String(localFile));
			System.out.println("Write to local file <"+file.getAbsolutePath()+">");
			FileOutputStream fos=new FileOutputStream(file);
			try{
				System.out.println("Reading <"+size+"> bytes.");
				long start = System.currentTimeMillis();
			    //and read the file data
				Util.copy(in, fos, size);
				long end = System.currentTimeMillis();
				long mb=size/(1024*1024);
				double mbytes=1000*mb/(end-start);
				System.out.println(client.getStatistics());
				
				System.out.println("Rate: "+(int)mbytes+" MBytes/sec.");
				double mbit=8*mbytes;
				System.out.println("Rate: "+(int)mbit+" MBit/sec.");
				
				
				client.shutdown();
				
			}finally{
				fos.close();
			}		
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	
	public static void main(String[] args) throws Exception{
		int serverPort=65321;
		String serverHost="localhost";
		String remoteFile="";
		String localFile="";
		
		try{
			serverHost=args[0];
			serverPort=Integer.parseInt(args[1]);
			remoteFile=args[2];
			localFile=args[3];
		}catch(Exception ex){
			usage();
			System.exit(1);
		}
		
		ReceiveFile rf=new ReceiveFile(serverHost,serverPort,remoteFile, localFile);
		rf.run();
	}
	
	public static void usage(){
		System.out.println("Usage: java -cp .. udt.util.ReceiveFile <server_ip> <server_port> <remote_filename> <local_filename>");
	}
	
}