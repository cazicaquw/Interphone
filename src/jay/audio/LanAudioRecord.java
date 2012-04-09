package jay.audio;

import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.interphone.InterphoneActivity;

import jay.dencode.Encoder;

import dalvik.system.TemporaryDirectory;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public class LanAudioRecord extends Thread{
	
	protected AudioRecord m_in_rec;
	protected int m_in_buf_size;
	protected short[] m_in_bytes;
	
	protected AudioTrack m_out_trk;
	protected int m_out_buf_size;
	protected byte[] m_out_bytes;
	protected boolean m_keep_running;
	
	protected DatagramSocket udp_socket;
	protected DataOutputStream dout;
	protected LinkedList<byte[]> m_in_q;
	protected int SampleRate;
	protected String destip;
	protected int destport;
	protected boolean muteflag;
	private final int framesize = 160;
	
	//used for echo calc
	private Logger log = LoggerFactory.getLogger(LanAudioPlay.class);
	private Encoder encoder ;
	
	public LanAudioRecord(DatagramSocket socket ,String destip){
		this.destip = destip;
		this.destport = socket.getLocalPort();
		SampleRate = 16000;
		//*************record_init********************
		m_in_buf_size = AudioRecord.getMinBufferSize(SampleRate,
						AudioFormat.CHANNEL_CONFIGURATION_MONO, 
						AudioFormat.ENCODING_PCM_16BIT);
		m_in_rec = new AudioRecord(MediaRecorder.AudioSource.MIC,
								  SampleRate, 
								  AudioFormat.CHANNEL_CONFIGURATION_MONO,
								  AudioFormat.ENCODING_PCM_16BIT,
								  m_in_buf_size*10);
		m_in_bytes = new short [framesize];
		
		System.out.println("m_in_bytes="+m_in_buf_size);
		
		m_in_q = new LinkedList<byte[]>();

		m_keep_running = true;
		muteflag = false;
		//*************network_init***********************
	
			udp_socket = socket;
			encoder = new Encoder(InterphoneActivity.codectype);
		
		
	}
	
	public void run(){
		
		

		try{
			short[] bytes_pkg = new short[framesize];
			/************Speex Encode Start***************/		
			
			Thread encodeThread = new Thread (encoder);
			encoder.setRecording(true);
			encodeThread.start();
			/************Speex Encode***************/
			m_in_rec.startRecording();
			while(m_keep_running){
				int bufferReadResult = m_in_rec.read(m_in_bytes, 0, framesize);
				bytes_pkg = m_in_bytes.clone();
				if(encoder.isIdle()){
					encoder.putData(System.currentTimeMillis(),bytes_pkg , bufferReadResult);
				}else {
					//认为编码处理不过来，直接丢掉这次读到的数据
//					log.error("encode:drop data!");
				}	
					if(muteflag == false && bufferReadResult >0){
						if(encoder.isGetData())
						{
							byte[] temp_getdata = encoder.getData().clone();
							DatagramPacket packet = new DatagramPacket(temp_getdata,temp_getdata.length ,InetAddress.getByName(this.destip),this.destport);
							//DatagramPacket packet = new DatagramPacket(temp_getdata,38 ,InetAddress.getByName(this.destip),this.destport);
							udp_socket.send(packet);
						}
					}
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void mute(){
		muteflag = true;
	}
	
	public void demute(){
		muteflag = false;
	}
	public int getport(){
		return udp_socket.getLocalPort();
	}
	public void free(){
		m_keep_running = false;
		m_in_rec.stop();
		m_in_rec.release();
		udp_socket.close();
		m_in_rec = null;
		m_in_bytes = null;
		encoder.free();
	}	
}