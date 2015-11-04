package alpv_ws1415.ub1.webradio.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import alpv_ws1415.ub1.webradio.communication.ServerTCP;

 

public class ServerGUI implements ServerUI {

	/*
	 * *(non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 * 
	 * Basic features : Change sound files and exits the program
	 */

	// GUI frames
	// Main menu
	private JFrame frame;
	private JFileChooser fileChooser; // To change the song path
	private JButton b1;
	private JButton b2;
	
	private int serverArgs ; 
	private Thread server;

	public ServerGUI(int arg) {
		serverArgs = arg;
	}

	private void shutdownServer() {

	}

	private class OnClickChangeSong implements ActionListener {
	

		@Override
		public void actionPerformed(ActionEvent arg0) {
			System.out.println("Click");
		}

	}
	
	 private class OnClickShutdownServer implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				System.setIn( new ByteArrayInputStream( "currentSong".getBytes("UTF-8") ));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
		 
	 }

	@Override
	public void run() {
		setUI();
		
		// Start the Server
		server = new Thread(new ServerTCP(serverArgs));
		server.start();

	}

	private void setUI() {
		// Imagines for the buttons
		ImageIcon shutdownServerIcon = new ImageIcon(ServerGUI.class.getResource("/Res/Img/shutdownServerIcon.gif"));
		ImageIcon changeSongIcon = new ImageIcon(ServerGUI.class.getResource("/Res/Img/changeSongIcon.gif"));

		// Create and set up the window.
		frame = new JFrame("Server");
		frame.getContentPane().setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo( null );
		frame.getContentPane().setPreferredSize(new Dimension(240, 60));
		frame.setResizable(false);
		


		// Create buttons
		b1 = new JButton( shutdownServerIcon);
		b1.setToolTipText("Click this button to shutdown server");
		b1.setBounds(10, 10, 40, 25);
		
		b2 = new JButton( changeSongIcon);
		b2.setToolTipText("Click this button to change song streaming on server");
		b2.setBounds(120 ,10, 40,25);
		
		// Listener for button clicks
		b1.addActionListener(new OnClickShutdownServer());
		b2.addActionListener(new OnClickChangeSong());



		// Add the buttons
		frame.getContentPane().add(b1);
		frame.getContentPane().add(b2);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	private class ServerWindow extends JFrame {

	}

}
