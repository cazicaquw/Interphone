package jay.codec;

import android.preference.ListPreference;
import jay.func.func;

public  class Codec {
	
	protected Speex speex;
	protected ulaw g711u; 
	protected alaw g711a;
	protected G722 g722;
	protected static int codeccode;
	protected final int framesize=160;
	protected final int DEFAULT_BITRATE = 64000;
	//choose what codec to use
	public Codec(int codeccode) {
		this.codeccode = codeccode;
	}
	
	public void init() {
		switch(this.codeccode){
		case 0:
			break;
		case 1:
			speex=new Speex();
			speex.init();
			break;
		case 2:
			g711u = new ulaw();
			g711u.init();
			break;
		case 3:
			g711a = new alaw();
			g711a.init();
			break;
		case 4:
			g722 = new G722();
			g722.init();
			break;
		}
	}
    //I put load() in related codec class
	public int open(int compression){
		switch(this.codeccode){
		case 0:
			return 0;
		case 1:
			return speex.open(compression);
		case 2:
			return 0;
		case 3:
			return 0;
		case 4:
			return g722.open(DEFAULT_BITRATE);
		default:
			return 0;
		}
	}
	public int getFrameSize()
	{
		switch(this.codeccode){
		case 0:
			return framesize;
		case 1:
			return speex.getFrameSize();
		case 2:
			return 0;
		case 3:
			return 0;
		case 4:
			return 0;
		default:
			return 0;
		}
	}
	public int decode(byte[] encoded, short[] lin, int size)
	{
		switch(this.codeccode){
		case 0:
			System.arraycopy(func.byteArray2ShortArray(encoded), 0, lin, 0, size/2);
			return size/2;
		case 1:
			return speex.decode(encoded, lin, size);
		case 2:
			return g711u.decode(encoded, lin, size);
		case 3:
			return g711a.decode(encoded, lin, size);
		case 4:
			return g722.decode(encoded, lin, size);
		default:
			return 0;
		}
	}
	public int encode(short[] lin, int offset, byte[] encoded, int size){
		switch(this.codeccode){
		case 0:
			System.arraycopy(func.shortArray2ByteArray(lin), 0, encoded, 0, size*2);
			return size*2;
		case 1:
			return speex.encode(lin, offset, encoded, size);
		case 2:
			return g711u.encode(lin, offset, encoded, size);
		case 3:
			return g711a.encode(lin, offset, encoded, size);
		case 4:
			return g722.encode(lin, offset, encoded, size);
		default:
			return 0;
		}
	}
	public void close(){
		switch(this.codeccode){
		case 0:
			break;
		case 1:
			speex.close();
			break;
		case 4:
			g722.close();
		default:
			break;
		}
	}

}
