package cns_controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.lang.*;

import javax.swing.JOptionPane;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import cns_main.CnsGui;
import cns_main.CnsSettings;
import config_utilities.Module;

public class ModuleStarter extends Thread{
	private Process p;
	private Session session;
	private Module module;
	private CnsSettings setting;
	private CnsGui gui;
	//private boolean running;
	
	public ModuleStarter (Module module, CnsSettings setting, CnsGui gui){
		this.module = module;
		this.setting = setting;
		this.gui = gui;
		module.resetOutput();
	}
	
	public void run ()
	{
		String cmd = module.getStartCommand();
		System.out.println("starting module "+module.getName()+" with :\""+cmd+"\" ");
		module.startedRunning();
		gui.getModuleTable().updateUI();
		//if (true){
		if (module.getComputer().isThisPC()){
			try {
				p = Runtime.getRuntime().exec(cmd);
				// read output with BufferedReader
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String temp = null;
				while( ((temp = br.readLine())!= null)){
					module.addToOutput(temp+"\n");
				}
				//System.out.println(module.getReader().readLine());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} /*catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		}
		else{
			try {

				//example from http://www.jcraft.com/jsch/examples/Exec.java.html
				JSch jsch=new JSch();  

				session=jsch.getSession(module.getComputer().getUser(), module.getComputer().getIpLan(), 22);
				
				//TODO this is not safe!! should not do this
				JSch.setConfig("StrictHostKeyChecking", "no");
				
				//UnknownHostKey: 192.168.188.101. RSA key fingerprint is 2c:3e:b0:0d:33:68:d7:c3:26:83:d2:21:21:46:4c:5f
				//jsch.setKnownHosts("C:\\Users\\leonie\\known_hosts");
				//String knownHostPublicKey = "192.168.188.101 ecdsa-sha2-nistp256 b7:de:f6:46:0b:a1:4f:90:4e:2f:ba:db:d1:a5:fb:4d";
				//String knownHostPublicKey = "192.168.188.101 ssh-rsa 2c3eb00d3368d7c32683d22121464c5f";
				//jsch.setKnownHosts(new ByteArrayInputStream(knownHostPublicKey.getBytes()));

				String pass = setting.getPassword(module.getComputer());
				if (pass == null) {
					pass = JOptionPane.showInputDialog(null, "Please enter password for " + module.getComputer().getUser() + " on " + module.getComputer().getName() + ":", "Enter password", JOptionPane.QUESTION_MESSAGE);
					if (pass == null) {
						System.out.println("Cancel");
						return;
					}
					setting.addPassword(module.getComputer(), pass);
					
				}
				
				session.setPassword(pass);
				session.connect();

				Channel channel=session.openChannel("exec");
				((ChannelExec)channel).setCommand(cmd);

				// X Forwarding
				// channel.setXForwarding(true);

				//channel.setInputStream(System.in);
				channel.setInputStream(null);

				//channel.setOutputStream(System.out);

				//FileOutputStream fos=new FileOutputStream("/tmp/stderr");
				//((ChannelExec)channel).setErrStream(fos);
				((ChannelExec)channel).setErrStream(System.err);

				InputStream in=channel.getInputStream();

				channel.connect();

				byte[] tmp=new byte[1024];
				while(true){
					while(in.available()>0){
						int i=in.read(tmp, 0, 1024);
						if(i<0)break;
						module.addToOutput(new String(tmp, 0, i));
					}
					if(channel.isClosed()){
						if(in.available()>0) continue; 
						module.addToOutput("exit-status: "+channel.getExitStatus());
						break;
					}
					try{Thread.sleep(1000);}catch(Exception ee){}
				}
				channel.disconnect();
				session.disconnect();
			}
			catch(Exception e){
				System.out.println(e);
			}
		}
		module.stoppedRunning();
		gui.getModuleTable().updateUI();
	}
	
	public void killModule ()
	{
		if (p!=null)
			p.destroy();
		if (session!=null)
			session.disconnect();
		module.stoppedRunning();
		gui.getModuleTable().updateUI();
	}
	


}
