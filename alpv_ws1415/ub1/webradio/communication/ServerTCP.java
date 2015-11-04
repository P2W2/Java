package alpv_ws1415.ub1.webradio.communication;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

import alpv_ws1415.ub1.webradio.audioplayer.AudioPlayer;

public class ServerTCP implements Server {
	/*
	 * 
	 * inner Classes which implements Runnable:
	 *
	 * ClientHandler - Handles incoming Clients and sets them up to listen to
	 * music
	 * 
	 * KeyListener - Listens on Console input to exit,change and start playing
	 * music
	 * 
	 * StreamPlayer - stream music to all Clients added to the clients
	 * collection
	 *
	 */

	// Strings
	private String hello = "Hello World";
	private String path = ".\\Res\\Wav\\music.wav"; // TODO Change to get wav
													// from classRessources
	private String USAGE = "USAGE: %n%nType 'exit' to shutdown server,%n" + ", 'playSong' to start musicstream%n"
			+ ", 'currentSong' for current playing song%n" + ", 'queueSong' to queue a Song%n"
			+ "or 'setSongPath' to change song path";
	private ArrayList<String> playList = new ArrayList<>();

	// Member variables
	private int port;
	private ServerSocket serverSocket;
	private AudioPlayer audioPlayer;
	private AudioInputStream ais;
	private volatile Collection<ClientHandler> clients = new ArrayList<ClientHandler>();
	private ExecutorService pool;
	private boolean musicIsPlaying = false;
	private Thread streamPlayerThread = null;
	private StreamPlayer streamPlayer = null;

	final int poolSize = 8;
	private volatile boolean serverRunning = true;
	private volatile boolean keyListenerRunning = true;
	private volatile boolean streamPlayerRunning = true;
	private volatile boolean changeTitle = false;

	// constructor
	public ServerTCP(int port) {
		this.port = port;
		this.pool = Executors.newFixedThreadPool(poolSize);
	}

	// server run
	public void run() {
		// New Server socket
		try {
			Thread listener = new Thread(new KeyListener());
			listener.start();
			System.out.printf("Starting Server\n");
			serverSocket = new ServerSocket(port);
			// Waiting for new client and be blocked while waiting
			while (serverRunning) {
				System.out.printf("Waiting for new Clients to Connect to Server\n");
				Socket client = serverSocket.accept();
				System.out.println("New Client, passing to Handler");
				pool.execute(new ClientHandler(client));
			}
		} catch (Exception e) {
			close();
		} finally {
			pool.shutdown();
			try {
				pool.awaitTermination(4L, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				System.out.println("Interrupt!");
				e.printStackTrace();
			}
		}

	}

	// close Server socket
	@Override
	public void close() {
		serverRunning = false;
		try {
			// closeOnAllClients();
			serverSocket.close();
		} catch (IOException e) {
			System.out.println("Error while closing ServerSocket");
			e.printStackTrace();
		}
	}

	public void closeOnAllClients() throws IOException {
		for (ClientHandler ch : clients) {
			ch.clientSoc.close();
		}
	}

	/*
	 * ---------------------------------------------------------------------
	 * ---- --------------------------------------------------
	 *
	 * sets audioplayer and stuff
	 *
	 * ---------------------------------------------------------------------
	 * ---- --------------------------------------------------
	 */
	public void setSongPath(String string) {
		path = string;
	}

	public void playSong(String path) throws MalformedURLException, UnsupportedAudioFileException, IOException {
		if (!playList.isEmpty()) {
			setSongPath(playList.remove(0));
		}
		if (musicIsPlaying) {
			changeTitle = true;
		} else {
			musicIsPlaying = true;
			ais = AudioPlayer.getAudioInputStream(path);
			setAudioPlayer(new AudioPlayer(ais.getFormat()));

			streamPlayer = new StreamPlayer();
			pool.execute(streamPlayer);
		}

	}

	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}

	public void setAudioPlayer(AudioPlayer audioPlayer) {
		this.audioPlayer = audioPlayer;
	}

	// Send some text
	private void sayHello(Socket client) throws IOException {
		// Send Hello World to client
		PrintWriter pW = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
		pW.print(hello);
		pW.flush();
	}

	/*
	 * -------------------------------------------------------------------------
	 * -------------------------------------------------- Handles Clients
	 *
	 * -------------------------------------------------------------------------
	 * --------------------------------------------------
	 */
	class ClientHandler implements Runnable {

		private Socket clientSoc = null;

		public ClientHandler() {
		}

		public ClientHandler(Socket socket) {
			clientSoc = socket;
		}

		@Override
		// ClientHandler run
		public void run() {
			try {
				// be polite and say hello first
				sayHello(clientSoc);
				// Get a new OutputStreamWriter
				DataOutputStream osw = new DataOutputStream(clientSoc.getOutputStream());
				// Send the client the audioFormat as a String;
				AudioFormat aF = AudioPlayer.getAudioInputStream(path).getFormat();
				// Send Client the format
				String audioFormatString = aF.getEncoding().toString() + ',' + aF.getSampleRate() + ','
						+ aF.getSampleSizeInBits() + ',' + aF.getChannels() + ',' + aF.getProperty("signed") + ','
						+ aF.isBigEndian();
				osw.writeUTF(audioFormatString);
				osw.flush();

				// Add this instance to collection
				synchronized (clients) {
					clients.add(this);
				}
			} catch (

			IOException e)

			{
				System.out.println("Error: Io exception");
				e.printStackTrace();
			} catch (

			UnsupportedAudioFileException e)

			{
				System.out.println("Error: Audio File is not supported");
				e.printStackTrace();
			}

		}

		public void terminate() throws IOException {
			clientSoc.close();
			synchronized (clients) {
				clients.remove(this);
			}
		}

	}

	/*
	 * ---------------------------------------------------------------------
	 * ---- --------------------------------------------------
	 *
	 * KeyListener waiting for input
	 *
	 * ---------------------------------------------------------------------
	 * ---- --------------------------------------------------
	 */
	class KeyListener implements Runnable {
		@Override
		public void run() {
			System.out.printf("%n%n%n" + USAGE + "%n%n%n");
			while (keyListenerRunning) {
				try {
					Thread.sleep(300L);
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					System.out.printf(">>");
					String line = in.readLine();
					if (line.equals("exit")) {
						streamPlayer.terminate();
						streamPlayerThread.join();
						in.close();
						close();
						keyListenerRunning = false;
					}
					if (line.equals("playSong")) {
						playSong(path);
					}
					if (line.equals("currentSong")) {
						System.out.println(path);
					}
					if (line.equals("setSongPath")) {
						System.out.printf("Set song path to ?%n%nNew path: ");
						setSongPath(in.readLine());
						playSong(path);
					}
					if (line.equals("queueSong")) {
						System.out.printf("Path to song to be queued ?%n%nPath : ");
						playList.add(in.readLine());
						System.out.printf("Current Playlist: " + playList + "%n%n");

					}
					if (line.equals("man")) {
						System.out.println(USAGE);
					}
				} catch (Exception e) {
					System.exit(0);
				}
			}

		}

		private void setSongPath(String line) {
			path = line;
		}

	}

	/*
	 * -------------------------------------------------------------------------
	 * -------------------------------------------------- Inner Class Streams
	 * Music to all clients in the collection
	 *
	 * -------------------------------------------------------------------------
	 * --------------------------------------------------
	 */
	class StreamPlayer implements Runnable {
		int nRead = 0;

		public void terminate() {
			streamPlayerRunning = false;
			musicIsPlaying = false;
		}

		@Override
		public void run() {
			while (streamPlayerRunning) {
				changeTitle = false;
				byte[] buffer = new byte[4096];
				File file = new File(path);
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(file);
					while ((nRead = fis.read(buffer, 0, 4096)) >= 0) {
						if (changeTitle) {
							fis.close();
						} else {
							synchronized (clients) {
								for (Iterator<ClientHandler> iter = clients.iterator(); iter.hasNext();) {
									try {
										ClientHandler client = (ClientHandler) iter.next();
										DataOutputStream outToClient = new DataOutputStream(
												client.clientSoc.getOutputStream());
										outToClient.writeInt(buffer.length);
										outToClient.write(buffer);
									} catch (Exception e) {
										System.out.println("Unit lost");
										iter.remove();
									}
								}
							}
						}
					}

				} catch (FileNotFoundException e) {
					System.out.printf("Couldn't find  %s .\n", path);
				} catch (IOException e) {
					System.out.printf("IO Exception found!\n");
				}
				try {
					fis.close();
				} catch (IOException e) {
					System.out.printf("Error on closing file!\n");
				}

			}
		}

	}
}
