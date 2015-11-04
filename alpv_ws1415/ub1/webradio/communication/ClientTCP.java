package alpv_ws1415.ub1.webradio.communication;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.sound.sampled.AudioFormat;

import alpv_ws1415.ub1.webradio.audioplayer.AudioPlayer;

public class ClientTCP implements Client {

	int port;
	String addr;
	String username;
	Socket host;
	private boolean musicIsPlaying;

	public ClientTCP(String addr, int port, String username) {
		this.addr = addr;
		this.username = username;
		this.port = port;
	}

	@Override
	public void run() {
		// connects to server
		try {
			InetSocketAddress sockAddr = new InetSocketAddress(addr, port);
			connect(sockAddr);
			// Hello World receiving and printing
			InputStreamReader serverIn = new InputStreamReader(host.getInputStream());
			BufferedReader bufferedReader = new BufferedReader(serverIn);
			char[] buffer = new char[100];
			int nRead = bufferedReader.read(buffer, 0, 100);
			System.out.println(new String(buffer, 0, nRead));
			
			// Get the AudioFormat
			System.out.println("Getting audioformat");
			DataInputStream din = new DataInputStream(host.getInputStream());
			String[] farr =din.readUTF().split(",");
			AudioFormat audioFormat = new AudioFormat(Float.parseFloat(farr[1]), Integer.parseInt(farr[2]),
					Integer.parseInt(farr[3]), true, Boolean.parseBoolean(farr[5]));
			System.out.print("The audioformat: " + audioFormat + '\n');

			// Start Player
			AudioPlayer audioPlayer = new AudioPlayer(audioFormat);
			audioPlayer.start();

			// Play Music
			while(musicIsPlaying){
				byte[] nData = readPacket(din);
				if (nData != null){
					audioPlayer.writeBytes(nData);
				}
			}
			din.close();
			close();

		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			close();
		}
	}

	private byte[] readPacket(DataInputStream din) {
		int packetLen 	= 0;
		byte[] data 	= null;
	
		try {
			// Get packet length
			packetLen = din.readInt();

			// Create data buffer
			data = new byte[packetLen];			
			if (packetLen > 0)
				din.readFully(data);
	
			return data;
		} catch (Exception e) {
			System.out.printf("Lost connection to server\n");
			musicIsPlaying = false; 
			return null;
		}
		
	}
	@Override
	public void connect(InetSocketAddress serverAddress) throws IOException {
		// connects to server
		try {
			host = new Socket(serverAddress.getHostName(), port);
			musicIsPlaying = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		try {
			host.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendChatMessage(String message) throws IOException {
		// TODO Auto-generated method stub

	}

}
