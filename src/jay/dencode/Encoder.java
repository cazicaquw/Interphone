package jay.dencode;

import java.util.LinkedList;

import jay.audio.LanAudioRecord;
import jay.codec.Codec;
import jay.codec.EchoCancellation;
import jay.codec.Speex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.interphone.InterphoneActivity;

public class Encoder implements Runnable {

	private Logger log = LoggerFactory.getLogger(Encoder.class);
	private volatile int leftSize = 0;
	private final Object mutex = new Object();
	//private Speex speex = new Speex();
	private Codec codec ;
	private int frameSize = 160;
	private long ts;
	private byte[] processedData = new byte[frameSize*2];
	private short[] rawdata = new short[frameSize];
	private volatile boolean isRecording;
	protected LinkedList<byte[]> m_in_q=new LinkedList<byte[]>();    //store processed data
	private EchoCancellation m_ec;
	static public int num_send;

	public Encoder(int codeccode) {
		super();
//		speex.init();
		codec =new Codec(codeccode);
		codec.init();
		frameSize = codec.getFrameSize();

	}

	public void run() {

		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		int getSize = 0;
		while (this.isRecording()) {

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
				short output[]=rawdata.clone();
				this.m_ec=InterphoneActivity.m_ec;
				if(m_ec!=null){
					if(Decoder.num_recv>0){ 
						if(num_send<20)   
						{
							num_send++;
						}
						else{
							m_ec.putData(true, output, output.length);
							if(m_ec.isGetData())
							{ 
								output=m_ec.getshortData();
							}
						}
					}
				}
				getSize = codec.encode(output, 0, processedData, leftSize);
				byte tempdata[] =new byte[getSize];
				System.arraycopy(processedData, 0, tempdata, 0, getSize);
				m_in_q.add(tempdata);
				setIdle();
			}
		}
	}

	public void putData(long ts, short[] data, int size) {
		synchronized (mutex) {
			this.ts = ts;
			System.arraycopy(data, 0, rawdata, 0, size);
			this.leftSize = size;
			mutex.notify();
		}
	}
	
	public byte[] getData(){
		return m_in_q.removeFirst();
	}

	public boolean isGetData()
	{
		return m_in_q.size() == 0 ?false : true; 
	}
	public boolean isIdle() {
		return leftSize == 0 ? true : false;
	}

	public void setIdle() {
		leftSize = 0;
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}
	public void free(){
		num_send=0;
		codec.close();
	}
}
