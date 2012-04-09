package jay.dencode;

import java.util.LinkedList;

import jay.audio.LanAudioPlay;
import jay.audio.LanAudioRecord;
import jay.codec.Codec;
import jay.codec.EchoCancellation;
import jay.codec.Speex;
import jay.func.func;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.interphone.InterphoneActivity;

import android.content.Context;

public class Decoder implements Runnable {

	private Logger log = LoggerFactory.getLogger(Decoder.class);
	private volatile int leftSize = 0;
	private final Object mutex = new Object();
	private Codec codec;
//	private Speex speex = new Speex();
	private int frameSize =160;
	private long ts;
	private short[] processedData = new short[frameSize];
	private byte[] rawdata = new byte[frameSize*2];
	private volatile boolean isPlaying;
	protected LinkedList<short[]> m_out_q=new LinkedList<short[]>();    //store processed data
	private EchoCancellation m_ec;
	static public int num_recv;

	public Decoder(int codeccode) {
		super();
		codec = new Codec(codeccode);
		codec.init();
//		speex.init();
		log.debug(codec.getFrameSize()+"");
	}

	public void run() {

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		int getSize = 0;
		while (this.isPlaying()) {

			synchronized (mutex) {
				while (isIdle()) {
					try {
						mutex.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			synchronized (mutex) {
				this.m_ec=InterphoneActivity.m_ec;
				byte[] raw_temp = new byte[leftSize];
				System.arraycopy(rawdata, 0, raw_temp, 0, raw_temp.length);
				//getSize = speex.decode(raw_temp, processedData, leftSize);
				getSize = codec.decode(raw_temp, processedData, leftSize);
				short[]buffer = processedData.clone();		
				if(m_ec!=null){ //&& m_ec.isCancelling()==true){
					if(num_recv<100)
						num_recv++;
					else{
						m_ec.putData(false, buffer, buffer.length);
					}
					
				}
				m_out_q.add(buffer);
				setIdle();
			}
		}
	}

	public void putData(long ts, byte[] data, int size) {
		synchronized (mutex) {
			this.ts = ts;
			System.arraycopy(data, 0, rawdata, 0, size);
			this.leftSize = size;
			mutex.notify();
		}
	}
	
	public boolean isGetData()
	{
		return m_out_q.size() == 0 ?false : true; 
	}
	
	public short[] getData(){
		return m_out_q.removeFirst();
	}

	public boolean isIdle() {
		return leftSize == 0 ? true : false;
	}

	public void setIdle() {
		leftSize = 0;
	}

	public void setPlaying(boolean isPlaying) {
		synchronized (mutex) {
			this.isPlaying = isPlaying;
			if (this.isPlaying) {
				mutex.notify();
			}
		}
	}

	public boolean isPlaying() {
		synchronized (mutex) {
			return isPlaying;
		}
	}
	
	public void free(){
		num_recv=0;
		codec.close();
		//speex.close();
	}
}
