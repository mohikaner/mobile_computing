package a1.sender;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import a1.ressources.Package;
import a1.sender.MainClient.Monitor;

public class ClientSender implements Runnable {
	private int rtsTimeout;
	private int lambda;
	private String destIP;
	private String sourceIP;
	private int port;
	private Monitor monitor = null;
	private int seqNumber = 1;

	public int getSeqNumber() {
		return seqNumber;
	}

	public ClientSender(String sourceIP, String destIP, int port,
			int rtsTimeout, int lambda, Monitor monitor) {
		this.rtsTimeout = rtsTimeout;
		this.sourceIP = sourceIP;
		this.destIP = destIP;
		this.lambda = lambda;
		this.port = port;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
			clientSocket.setBroadcast(true);
		} catch (SocketException e2) {
			e2.printStackTrace();
		}
		// send one datagram
		while (true) {
			try {
				synchronized (this) {
					double random = Math.random();
					long waitingTime;
					do {
						random = Math.random();
						waitingTime = (long) ((-1.0 / lambda)
								* Math.log(random) * 1000);
					} while (waitingTime <= 0);
					System.out.println("Warte vor Senden f�r " + waitingTime);
					wait(waitingTime);
				}

			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Package rtsPackage = new Package();
				// rts
				rtsPackage.setId(0);
				rtsPackage.setDestIP(destIP);
				rtsPackage.setSourceIP(sourceIP);

				// paket
				DatagramPacket packet = createBytePackage(rtsPackage);
				clientSocket.send(packet);
				System.out.println("RTS gesendet warte auf Best�tigung");
				synchronized (monitor) {
					monitor.wait(rtsTimeout);
				}
				if (monitor.isCts()) {
					// data package
					System.out
							.println("CTS erhalten sende Datenpaket mit Nummer "
									+ seqNumber);
					Package dataPackage = new Package();
					dataPackage.setSeqNumber(seqNumber);
					dataPackage.setId(2);
					dataPackage.setDestIP(destIP);
					dataPackage.setSourceIP(sourceIP);
					DatagramPacket sendPackage = createBytePackage(dataPackage);
					clientSocket.send(sendPackage);
					seqNumber++;
					monitor.setCts(false);
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private DatagramPacket createBytePackage(Package p) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(1472);
		ObjectOutputStream os = new ObjectOutputStream(
				new BufferedOutputStream(byteStream));
		os.flush();
		os.writeObject(p);
		os.flush();
		// retrieves byte array
		byte[] sendBuf = byteStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length,
				InetAddress.getByName("192.168.1.255"), port);
		os.close();
		return packet;
	}
}
