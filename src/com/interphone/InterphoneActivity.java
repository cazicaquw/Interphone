package com.interphone;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import jay.audio.LanAudioPlay;
import jay.audio.LanAudioRecord;
import jay.codec.EchoCancellation;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


public class InterphoneActivity extends Activity {

	protected TextView m_text_ip;
	protected EditText m_text_destip;
	protected CheckBox m_muteflag;
	protected CheckBox m_echoflag;
	protected DatagramSocket udp_socket;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
    	menu.add(0,MENU_START_ID,0,"START");
    	menu.add(0,MENU_STOP_ID,0,"STOP");
    	menu.add(0,MENU_EXIT_ID,0,"EXIT");
		return super.onCreateOptionsMenu(menu);
		
	}

	public static final int MENU_START_ID =Menu.FIRST;
	public static final int MENU_STOP_ID = Menu.FIRST+1;
	public static final int MENU_EXIT_ID =Menu.FIRST+2;
	
	protected LanAudioPlay m_iPlay;
	protected LanAudioRecord m_iRecord;
	static public EchoCancellation m_ec;
	protected RadioButton radio0,radio1,radio2,radio3,radio4;
	protected RadioGroup radiogroup;
	public static int codectype=1;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m_text_ip = (TextView)findViewById(R.id.Myip);
        m_text_destip = (EditText)findViewById(R.id.Destip);
        m_muteflag = (CheckBox)findViewById(R.id.mutebox);
        m_echoflag = (CheckBox)findViewById(R.id.echobox);

        m_muteflag.setEnabled(false);
        m_muteflag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked)m_iRecord.mute();
				else m_iRecord.demute();
			}
		});
        m_echoflag.setEnabled(false);
        m_echoflag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked)
					m_ec.setCancelling(false);
				else m_ec.setCancelling(true);
			}
		});
        
		m_text_ip.setText(getLocalIpAddress());
	    InputFilter[] filters = new InputFilter[1];
	    filters[0] = new InputFilter() {
	        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
	            if (end > start) {
	                String destTxt = dest.toString();
	                String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
	                if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) { 
	                    return "";
	                } else {
	                    String[] splits = resultingTxt.split("\\.");
	                    for (int i=0; i<splits.length; i++) {
	                        if (Integer.valueOf(splits[i]) > 255) {
	                            return "";
	                        }
	                    }
	                }
	            }
	        return null;
	        }
	    };
	    m_text_destip.setFilters(filters);
    	radiogroup =(RadioGroup)findViewById(R.id.radioGroup1);
    	radio0 = (RadioButton)findViewById(R.id.radio0);
    	radio1 = (RadioButton)findViewById(R.id.radio1);
    	radio2 = (RadioButton)findViewById(R.id.radio2); 
    	radio3 = (RadioButton)findViewById(R.id.radio3);
    	radio4 = (RadioButton)findViewById(R.id.radio4);
    	
	    radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {  
            
            @Override  
            public void onCheckedChanged(RadioGroup group, int checkedId) {  
                // TODO Auto-generated method stub  
                if(checkedId == radio0.getId())
                	codectype = 0;
                else if(checkedId == radio1.getId())
                	codectype = 1;
                else if(checkedId == radio2.getId())
                	codectype = 2;
                else if(checkedId == radio3.getId())
                	codectype = 3;
                else if(checkedId == radio4.getId())
                	codectype = 4;
				}  
        }); 
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case MENU_START_ID:{
			if(m_text_destip.getText().toString().equals("")==false){
				new Thread (new audiostart()).start();
				m_muteflag.setEnabled(true);
				m_echoflag.setEnabled(true);
				m_text_destip.setEnabled(false);
			}
		}
		break;
		case MENU_STOP_ID:{
			new Thread(new audioclose()).start();
			m_muteflag.setEnabled(false);
			m_text_destip.setEnabled(true);
			
		}
		break;
		case MENU_EXIT_ID:{
			udp_socket.close();
			int pid = android.os.Process.myPid();
			android.os.Process.killProcess(pid);
		}
		break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	public class audiostart extends Thread{
		public void run(){
			try {
				udp_socket = new DatagramSocket(56434);
				System.out.println(udp_socket.getLocalPort());
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(m_iRecord==null && m_iPlay==null){
			
			m_iRecord = new LanAudioRecord(udp_socket,m_text_destip.getText().toString());
		    m_iPlay = new LanAudioPlay(udp_socket);
		    m_ec = new EchoCancellation();
			m_iPlay.start();
			m_iRecord.start();
			m_ec.setCancelling(true);
			new Thread(m_ec).start();	
			}
		}
	}
	
	public class audioclose extends Thread{
		public void run(){
			m_iPlay.free();
			m_iRecord.free();
			m_ec.free();
			if(m_iPlay!=null)
			m_iPlay = null;
			if(m_iRecord!=null)
			m_iRecord = null;
			if(m_ec!=null)
				m_ec=null;
		}
	}
	public String getLocalIpAddress() {  
        String ipaddress="";
        
    try {  
        for (Enumeration<NetworkInterface> en = NetworkInterface  
                .getNetworkInterfaces(); en.hasMoreElements();) {  
            NetworkInterface intf = en.nextElement();  
            for (Enumeration<InetAddress> enumIpAddr = intf  
                    .getInetAddresses(); enumIpAddr.hasMoreElements();) {  
                InetAddress inetAddress = enumIpAddr.nextElement();  
                if (!inetAddress.isLoopbackAddress()) {  
                        ipaddress=inetAddress.getHostAddress().toString();  
                }  
            }  
        }  
    } catch (SocketException ex) {  
          
    }  
    return ipaddress; 
    }
	
    
}