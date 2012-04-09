package jay.audio;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;

import jay.dencode.Decoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.interphone.InterphoneActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public  class LanAudioPlay extends Thread{
	protected AudioTrack m_out_trk;
	protected int m_out_buf_size;
	protected byte[] m_out_bytes;
	protected DatagramSocket udp_socket;
	protected boolean m_keep_running;
	protected int SampleRate;
	protected int listenport;
	protected LinkedList<byte[]> m_out_q;
	private final int framesize = 320;
	
	private Logger log = LoggerFactory.getLogger(LanAudioPlay.class);
	private Decoder decoder;
	//used for echo calc
	
    public LanAudioPlay(DatagramSocket socket){
    	try{
    		SampleRate=16000;
    		m_out_buf_size = AudioTrack.getMinBufferSize(SampleRate,
    									AudioFormat.CHANNEL_CONFIGURATION_MONO,
    									AudioFormat.ENCODING_PCM_16BIT);
    		m_out_trk = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
    									SampleRate,
    									AudioFormat.CHANNEL_CONFIGURATION_MONO,
    									AudioFormat.ENCODING_PCM_16BIT,
    									m_out_buf_size*5,
    									AudioTrack.MODE_STREAM);
    		m_out_bytes = new byte[framesize]; 	
    		
    		//*************network_init***********************
    		udp_socket = socket;
    		m_keep_running = true;
    		m_out_q = new LinkedList<byte[]>();
    		decoder = new Decoder(InterphoneActivity.codectype);
    	}
    	catch (Exception e) {
			// TODO: handle exception
    		e.printStackTrace();
		}
    }
    
    public void run(){
    	
    	DatagramPacket packet = new DatagramPacket(m_out_bytes, framesize);
    	//byte[] bytes_pkg=null;
		/************Speex Decode Start***************/	
		Thread decoderthread = new Thread (decoder);
		decoder.setPlaying(true);
		decoderthread.start();
    	
		/************Speex Decode***************/
    	m_out_trk.play();

    	while(m_keep_running){
    		try{
    			udp_socket.receive(packet);
    			byte [] bytes_pkg = new byte[packet.getLength()];
    			System.arraycopy(m_out_bytes, 0, bytes_pkg, 0, bytes_pkg.length);
//    			short[] s_bytes_pkg =new short[256];
//    			decoder.decode_test(bytes_pkg, s_bytes_pkg, bytes_pkg.length);
    			if(decoder.isIdle()){
					decoder.putData(System.currentTimeMillis(),bytes_pkg , bytes_pkg.length);
				}else {
					//认为编码处理不过来，直接丢掉这次读到的数据
//					log.error("decode:drop data!");
				}	
    			if(decoder.isGetData()==true){
    				short[] s_bytes_pkg = decoder.getData().clone();
   				m_out_trk.write(s_bytes_pkg, 0, s_bytes_pkg.length);}
    	//			m_out_trk.write(bytes_pkg, 0, bytes_pkg.length);
    			
    			
//    	    	packet = new DatagramPacket(m_out_bytes, m_out_buf_size);
    		}
    		catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	
//        bytes_pkg=null;
		
    }

    public void free(){
    	m_keep_running=false;
    	m_out_trk.stop();
    	m_out_trk.release();
    	udp_socket.close();
    	decoder.free();
    	m_out_trk = null;
    	m_out_bytes=null;
	}	
	
    
}