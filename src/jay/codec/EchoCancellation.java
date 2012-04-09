package jay.codec;

import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;

import jay.func.func;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoCancellation implements Runnable{

	private Logger log = LoggerFactory.getLogger(EchoCancellation.class);
	private Speex speex_echo = new Speex();
	private final Object mutex = new Object();
	final static int framesize = 160;
	final static int filterlength = 1024;
	private volatile int CaptureSize = 0;
	private volatile int PlaySize = 0;
	private volatile boolean isCancelling;
	protected LinkedList<short[]> m_cap_q=new LinkedList<short[]>();    //store processed data
	protected LinkedList<short[]> m_play_q=new LinkedList<short[]>();    //store processed data
	protected LinkedList<short[]> m_out_q=new LinkedList<short[]>();    //store processed data
	
	public EchoCancellation(){
		speex_echo.echoinit(framesize, filterlength);
//		log.debug("start echo cancellation");
	}
	
	public short[] echo_capture(short[] capture)
	{
//		log.debug("start echo playback");
		short[] buffer = new short[framesize];	
		speex_echo.echocapture(capture,buffer);
//		log.debug("echo capture done");
		return buffer;
	}
	public void echo_playback(short[] play)
	{
//		log.debug("start echo playback");
		speex_echo.echoplayback(play);
//		log.debug("echo playback");
		
	}
	
	public void free()
	{
		if(speex_echo!=null)
		speex_echo.echoclose();
	}

	public void run() {
		android.os.Process
		.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		while (this.isCancelling()) {
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
//				log.debug("echo thread");
				if(CaptureSize!=0)
				{
					m_out_q.add(echo_capture(m_cap_q.removeFirst()));
					
				}
				if(PlaySize!=0)
				{
//					log.debug("play");
					echo_playback(m_play_q.removeFirst());
				}
				
				setIdle();
//				log.debug("set idle");
			}
		}

		
		
		// TODO Auto-generated method stub
		
	}
	
	public void putData(boolean type,short[] data, int size) {
		synchronized (mutex) {
			short[] temp = new short[size];
			System.arraycopy(data, 0, temp, 0, size);
			if(type ==true)  //cap
				{
					m_cap_q.add(temp);
					this.CaptureSize = size;
				}
			else
				{
					m_play_q.add(temp);
					this.PlaySize = size;
				}
			mutex.notify();
		}
	}
	
	public void putData(boolean type,byte[] data, int size) {
		synchronized (mutex) {
			int shortsize=size/2;
			short buffer[] = new short [shortsize]; 
			buffer=func.byteArray2ShortArray(data);
			short[] temp = new short[shortsize];
			System.arraycopy(buffer, 0, temp, 0, shortsize);
			if(type ==true)  //cap
				{
					m_cap_q.add(temp);
					this.CaptureSize = shortsize;
				}
			else
				{
					m_play_q.add(temp);
					this.PlaySize = shortsize;
				}
			mutex.notify();
		}
	}
	
	public boolean isGetData()
	{
		return m_out_q.size() == 0 ?false : true; 
	}
	
	public short[] getshortData(){
		return m_out_q.removeFirst();
	}
	
	public byte[] getbyteData(){
		return func.shortArray2ByteArray(m_out_q.removeFirst());
	}
	
	public boolean isIdle() {
		return (CaptureSize == 0 && PlaySize==0) ? true : false;
	}
	public void setCancelling(boolean isCancelling) {
		synchronized (mutex) {
			this.isCancelling = isCancelling;
			if (this.isCancelling) {
				log.debug("set echo cancel***************************");
				mutex.notify();
			}
		}
	}

	public boolean isCancelling() {
		synchronized (mutex) {
			return isCancelling;
		}
	}
	
	public void setIdle()
	{
		if(CaptureSize!=0)CaptureSize=0;
		if(PlaySize!=0)PlaySize=0;
	}
}