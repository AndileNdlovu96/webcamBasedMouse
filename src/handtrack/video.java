package handtrack;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class video extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JFrame frame=new JFrame("Hand track");
	private JLabel lab = new JLabel();
	private static String stringa="expected action";

	private static Point last=new Point();
	private static boolean close=false;
	private static boolean act=false;
	private static long current=0;
	private static long prev=0;
	private static boolean start=false;
	/**
	 * Create the panel.
	 */
	public video() {

	}

	public void setframe(final VideoCapture webcam){
		frame.setSize(1024,768);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.getContentPane().add(lab);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				System.out.println("Closed");
				close=true;
				webcam.release();
				e.getWindow().dispose();
			}
		});
	}

	public void frametolabel(Mat matframe){
		MatOfByte cc=new MatOfByte();
		Imgcodecs.imencode(".JPG", matframe, cc);
		byte[] chupa= cc.toArray();
		InputStream ss=new ByteArrayInputStream(chupa);
		try {
			BufferedImage aa= ImageIO.read(ss);
			lab.setIcon(new ImageIcon(aa));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public double calculateDistance(Point P1,Point P2){
		double distance= Math.sqrt(((P1.x-P2.x)*(P1.x-P2.x))+((P1.y-P2.y)*(P1.y-P2.y)));

		return distance;
	}

	public double calculateAngle(Point P1,Point P2,Point P3){
		double angleInDegrees=0;
		Point v1=new Point();
		Point v2=new Point();
		v1.x=P3.x-P1.x;
		v1.y=P3.y-P1.y;
		v2.x=P3.x-P2.x;
		v2.y=P3.y-P2.y;
		double dotproduct = (v1.x*v2.x) + (v1.y*v2.y); 
		double length1 = Math.sqrt((v1.x*v1.x)+(v1.y*v1.y));
		double length2 = Math.sqrt((v2.x*v2.x)+(v2.y*v2.y));
		double angleInRadians = Math.acos(dotproduct/(length1*length2));
		angleInDegrees=angleInRadians*180/Math.PI;

		return angleInDegrees;
	}
	public Mat colorFilterInBGR(int b,int g,int r,int b1,int g1,int r1,Mat image){
		Mat modificationtion=new Mat();
		if(image!=null){
			Core.inRange(image, new Scalar(b,g,r), new Scalar(b1,g1,r1), modificationtion);
		}
		else{
			System.out.println("Image error - BGR filtering");
		}
		return modificationtion;
	}

	public Mat colorFilterInHSV(int h,int s,int v,int h1,int s1,int v1,Mat image){
		Mat modificationtion =new Mat();
		if(image!=null){
			Core.inRange(image, new Scalar(h,s,v), new Scalar(h1,s1,v1), modificationtion);
		}
		else{
			System.out.println("Image Error");
		}
		return modificationtion;
	}

	public Mat skindetection(Mat orig){
		Mat mask=new Mat();
		Mat result=new Mat();
		Core.inRange(orig, new Scalar(0,0,0),new Scalar(30,30,30),result);
		Imgproc.cvtColor(orig, mask, Imgproc.COLOR_BGR2HSV);
		for(int i=0;i<mask.size().height;i++){
			for(int j=0;j<mask.size().width;j++){
				if(mask.get(i,j)[0]<19 || mask.get(i, j)[0]>150
						&& mask.get(i,j)[1]>25 && mask.get(i,j)[1]<220){

					result.put(i,j,255,255,255);//white

				}
				else{
					result.put(i, j, 0,0,0);//black
				}
			}

		}


		return result;

	}

	public Mat morphologicalFilter(int kd,int ke,Mat image){
		Mat modificationtion =new Mat();
		Imgproc.erode(image, modificationtion, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(ke,ke)));
		Imgproc.dilate(modificationtion, modificationtion,  Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(kd,kd)));
		return modificationtion;

	}

	public List<MatOfPoint> searchForContours(Mat original, Mat image,boolean draw, boolean drawnAll, int pixelFilter){
		List<MatOfPoint> contours = new LinkedList<MatOfPoint>();
		List<MatOfPoint> contoursbig = new LinkedList<MatOfPoint>();
		Mat hierarchy= new Mat();

		Imgproc.findContours(image,contours , hierarchy ,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));

		for(int i=0;i<contours.size();i++) {
			if(contours.get(i).size().height>pixelFilter){
				contoursbig.add(contours.get(i));
				if(draw && !drawnAll)
					Imgproc.drawContours(original, contours,i,new Scalar(0,255,0),2,8,hierarchy,0,new Point());
			}

			if(drawnAll && !draw)
				Imgproc.drawContours(original, contours,i,new Scalar(0,255,255),2,8,hierarchy,0,new Point());

		}
		return contoursbig;
	}

	public List<Point> listOfContours(Mat image,int pixelFilter){
		List<MatOfPoint> contours = new LinkedList<MatOfPoint>();
		List<MatOfPoint> contoursbig = new LinkedList<MatOfPoint>();
		List<Point> listOfPoints=new LinkedList<Point>();
		Mat hierarchy= new Mat();

		Imgproc.findContours(image,contours , hierarchy ,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE,new Point(0,0));

		for(int i=0;i<contours.size();i++) {
			//System.out.println("Dimensione contorni"+contours.get(i).size().height);
			if(contours.get(i).size().height>pixelFilter){
				contoursbig.add(contours.get(i));
			}

		}
		if(contoursbig.size()>0){

			listOfPoints=contoursbig.get(0).toList();

		}
		return listOfPoints;
	}

	public List<Point> envelopeDefects(Mat image,List<MatOfPoint> contours,boolean draw,int depthThesholdForFingers){
		List<Point> defects=new LinkedList<Point>();

		for(int i=0;i<contours.size();i++){
			MatOfInt hull_=new MatOfInt();
			MatOfInt4 convexityDefects=new MatOfInt4();

			@SuppressWarnings("unused")
			List<Point> contourPoints = new LinkedList<Point>();
			contourPoints=contours.get(i).toList();

			Imgproc.convexHull(contours.get(i), hull_);

			if (hull_.size().height>=4){


				Imgproc.convexityDefects(contours.get(i), hull_, convexityDefects);

				List<Point> pts=new ArrayList<Point>();
				MatOfPoint2f pr=new MatOfPoint2f();
				Converters.Mat_to_vector_Point(contours.get(i), pts);
				//rettangle
				pr.create((int)(pts.size()), 1, CvType.CV_32S);
				pr.fromList(pts);
				if(pr.height()>10){
					RotatedRect r=Imgproc.minAreaRect(pr);
					Point[] rect=new Point[4];
					r.points(rect);

					Imgproc.line(image, rect[0], rect[1],new Scalar(0,100,0),2);
					Imgproc.line(image, rect[0], rect[3],new Scalar(0,100,0),2);
					Imgproc.line(image, rect[1], rect[2],new Scalar(0,100,0),2);
					Imgproc.line(image, rect[2], rect[3],new Scalar(0,100,0),2);
					Imgproc.rectangle(image, r.boundingRect().tl(), r.boundingRect().br(), new Scalar(50,50,50));				
				}
				//fine rettangle

				int[] buff = new int[4];
				int[] zx=new int[1];
				int[] zxx=new int[1];
				for(int i1=0;i1<hull_.size().height;i1++){
					if(i1<hull_.size().height-1){
						hull_.get(i1,0,zx);
						hull_.get(i1+1,0,zxx);
					}
					else
					{
						hull_.get(i1,0,zx);
						hull_.get(0,0,zxx);
					}
					if(draw)
						Imgproc.line(image, pts.get(zx[0]), pts.get(zxx[0]), new Scalar(140,140,140),2);
				}


				for(int i1=0;i1<convexityDefects.size().height;i1++){
					convexityDefects.get(i1, 0,buff);
					if(buff[3]/256>depthThesholdForFingers){
						if(pts.get(buff[2]).x>0 && pts.get(buff[2]).x<1024 && pts.get(buff[2]).y>0 && pts.get(buff[2]).y<768){
							defects.add(pts.get(buff[2]));
							Imgproc.circle(image, pts.get(buff[2]), 6, new Scalar(0,255,0));
							if(draw)
								Imgproc.circle(image, pts.get(buff[2]), 6, new Scalar(0,255,0));

						}
					}
				}
				if (defects.size()<3){
					int dim=pts.size();
					Imgproc.circle(image, pts.get(0), 3, new Scalar(0,255,0),2);
					Imgproc.circle(image, pts.get(0+dim/4), 3, new Scalar(0,255,0),2);
					defects.add(pts.get(0));
					defects.add(pts.get(0+dim/4));


				}
			}
		}
		return defects;
	}

	public Point centerOfPalm(Mat image,List<Point> defects){
		MatOfPoint2f pr=new MatOfPoint2f();
		Point center=new Point();
		float[] radius=new float[1];
		pr.create((int)(defects.size()), 1, CvType.CV_32S);
		pr.fromList(defects);

		if(pr.size().height>0){
			start=true;
			Imgproc.minEnclosingCircle(pr, center, radius);

			Imgproc.circle(image, center,(int) radius[0], new Scalar(255,0,0));
			Imgproc.circle(image, center, 3, new Scalar(0,0,255),4);
		}
		else{
			start=false;
		}
		return center;

	}

	public List<Point> fingers(Mat image,List<Point> contourPoints,Point center){
		List<Point> pointsOfFingers=new LinkedList<Point>();
		List<Point> fingers=new LinkedList<Point>();
		int interval=55;
		for(int j=0;j<contourPoints.size();j++){
			Point prec=new Point();
			Point vertice=new Point();
			Point next=new Point();
			vertice=contourPoints.get(j);
			if(j-interval>0){

				prec=contourPoints.get(j-interval);
			}
			else{
				int a=interval-j;
				prec=contourPoints.get(contourPoints.size()-a-1);
			}
			if(j+interval<contourPoints.size()){
				next=contourPoints.get(j+interval);
			}
			else{
				int a=j+interval-contourPoints.size();
				next=contourPoints.get(a);
			}

			Point v1= new Point();
			Point v2= new Point();
			v1.x=vertice.x-next.x;
			v1.y=vertice.y-next.y;
			v2.x=vertice.x-prec.x;
			v2.y=vertice.y-prec.y;
			double dotproduct = (v1.x*v2.x) + (v1.y*v2.y); 
			double length1 = Math.sqrt((v1.x*v1.x)+(v1.y*v1.y));
			double length2 = Math.sqrt((v2.x*v2.x)+(v2.y*v2.y));
			double angle = Math.acos(dotproduct/(length1*length2));
			angle=angle*180/Math.PI;
			if(angle<60)
			{
				double centerprec=Math.sqrt(((prec.x-center.x)*(prec.x-center.x))+((prec.y-center.y)*(prec.y-center.y)));
				double centervert=Math.sqrt(((vertice.x-center.x)*(vertice.x-center.x))+((vertice.y-center.y)*(vertice.y-center.y)));
				double centernext=Math.sqrt(((next.x-center.x)*(next.x-center.x))+((next.y-center.y)*(next.y-center.y)));
				if(centerprec<centervert && centernext<centervert){

					pointsOfFingers.add(vertice);
					//Core.circle(image, vertice, 2, new Scalar(200,0,230));

					//Core.line(image, vertice, center, new Scalar(0,255,255));
				}
			}
		}

		Point average=new Point();
		average.x=0;
		average.y=0;
		int med=0;
		boolean t=false;
		if(pointsOfFingers.size()>0){
			double dif=Math.sqrt(((pointsOfFingers.get(0).x-pointsOfFingers.get(pointsOfFingers.size()-1).x)*(pointsOfFingers.get(0).x-pointsOfFingers.get(pointsOfFingers.size()-1).x))+((pointsOfFingers.get(0).y-pointsOfFingers.get(pointsOfFingers.size()-1).y)*(pointsOfFingers.get(0).y-pointsOfFingers.get(pointsOfFingers.size()-1).y)));
			if(dif<=20){
				t=true;
			}
		}
		for(int i=0;i<pointsOfFingers.size()-1;i++){

			double d=Math.sqrt(((pointsOfFingers.get(i).x-pointsOfFingers.get(i+1).x)*(pointsOfFingers.get(i).x-pointsOfFingers.get(i+1).x))+((pointsOfFingers.get(i).y-pointsOfFingers.get(i+1).y)*(pointsOfFingers.get(i).y-pointsOfFingers.get(i+1).y)));

			if(d>20 || i+1==pointsOfFingers.size()-1){
				Point p=new Point();

				p.x=(int)(average.x/med);
				p.y=(int)(average.y/med);

				//if(p.x>0 && p.x<1024 && p.y<768 && p.y>0){

					fingers.add(p);
				//}

				if(t && i+1==pointsOfFingers.size()-1){
					Point ult=new Point();
					if(fingers.size()>1){
						ult.x=(fingers.get(0).x+fingers.get(fingers.size()-1).x)/2;
						ult.y=(fingers.get(0).y+fingers.get(fingers.size()-1).y)/2;
						fingers.set(0, ult);
						fingers.remove(fingers.size()-1);
					}
				}
				med=0;
				average.x=0;
				average.y=0;
			}
			else{

				average.x=(average.x+pointsOfFingers.get(i).x);
				average.y=(average.y+pointsOfFingers.get(i).y);
				med++;


			}
		}


		return fingers;
	}

	public void drawFingersToCenterOfPalm(Mat image,Point center,Point finger,List<Point> fingers){

		Imgproc.line(image,new Point(150,50),new Point(730,50), new Scalar(255,0,0),2);
		Imgproc.line(image,new Point(150,380),new Point(730,380), new Scalar(255,0,0),2);
		Imgproc.line(image,new Point(150,50),new Point(150,380), new Scalar(255,0,0),2);
		Imgproc.line(image,new Point(730,50),new Point(730,380), new Scalar(255,0,0),2);
		if(fingers.size()==1){
			Imgproc.line(image, center, finger, new Scalar(0, 255, 255),4);
			Imgproc.circle(image, finger, 3, new Scalar(255,0,255),3);
			//Core.putText(image, finger.toString(), finger, Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0,200,255));

		}
		else
		{
			for(int i=0;i<fingers.size();i++){
				Imgproc.line(image, center, fingers.get(i), new Scalar(0, 255, 255),4);
				Imgproc.circle(image, fingers.get(i), 3, new Scalar(255,0,255),3);
			}
		}
		Imgproc.circle(image, center, 3, new Scalar(0,0,255),3);
		//Core.putText(image, center.toString(), center, Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(0,200,255));

	}

	public void mousetrack(List<Point> fingers,Point finger,Point center,Robot r,boolean on,Mat image, long temp) throws InterruptedException{

		if(on && center.x>10 && center.y>10 && finger.x>10 && center.y>10 && start){
			current=temp;
			switch(fingers.size()){
			case 0: 
				if(act && current-prev>500){
					stringa="Drag & drop";
					r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					act=false;
				}
				else{
					if(current-prev>500){
						Point p=new Point();
						Point np=new Point();
						np.x=center.x-last.x;
						np.y=center.y-last.y;
						p.x=(int)(-1*(np.x-730))*1366/580;
						p.y=(int)(np.y-50)*768/330;
						if(p.x>0 && p.x>0 && p.x<1367 && p.y<769){
							r.mouseMove((int)p.x,(int)p.y);
						}
						
					}
				}
				break;
			case 1: 
				

				if(act && current-prev>500){
					stringa="Click";
					System.out.println("click");
					r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					
				
					r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					
					r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					System.out.println("released");

					act=false;
				}
				else{
					if(current-prev>500){
						stringa="cursor";

						Point p1=new Point();
						p1.x=(int)(-1*(finger.x-730))*1366/580;
						p1.y=(int)(finger.y-50)*768/330;
						if(p1.x>0 && p1.x>0 && p1.x<1367 && p1.y<769){
							r.mouseMove((int)p1.x,(int)p1.y);
						}
						last.x=center.x-finger.x;
						last.y=center.y-finger.y;
					}
				}
				break;
			case 2: 
				double angle=calculateAngle(fingers.get(0),fingers.get(1),center);
				r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
				if(act && current-prev>500){
					act=false;
					if((int)angle<30){
						stringa="double click";
						System.out.println("double click");
						r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
						r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
						r.delay(100);
						r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
						r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					}
					else{
						stringa="right click";
						r.mousePress(InputEvent.BUTTON3_DOWN_MASK);
						r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
					}
					
				}
				break;
			case 3:
				stringa="undo";
				act=false;
				break;
			case 4:stringa="block cursor: expected action!";
			r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

			prev=temp;
			act=true;

			break;
				
			case 5: stringa="block cursor: expected action!";
			if(!act){
			r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

			prev=temp;
			act=true;
			}
			break;
			default: stringa="expected action";
			
			break;
			}

		}
		else{
			r.mouseRelease(InputEvent.BUTTON1_MASK);
		}
		Imgproc.putText(image,stringa,new Point(50,40), Core.FONT_HERSHEY_COMPLEX, 1, new Scalar(200,0,0));

	}

	public Point filterAverageMovement(List<Point> buffer, Point attuale){
		Point average=new Point();
		average.x=0;
		average.y=0;
		for(int i=buffer.size()-1;i>0;i--){
			buffer.set(i, buffer.get(i-1));
			average.x=average.x+buffer.get(i).x;
			average.y=average.y+buffer.get(i).y;
		}
		buffer.set(0, attuale);
		average.x=(average.x+buffer.get(0).x)/buffer.size();
		average.y=(average.y+buffer.get(0).y)/buffer.size();
		return average;
	}



	public static void main(String[] args) throws InterruptedException, AWTException {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		video v=new video();
		VideoCapture webcam=new VideoCapture(0);
		webcam.set(Videoio.CV_CAP_PROP_FRAME_HEIGHT,768 );
		webcam.set(Videoio.CV_CAP_PROP_FRAME_WIDTH,1024);
		v.setframe(webcam);
		Robot r=new Robot();
		Mat mimm = new Mat();
		Mat modification=new Mat();
		Point center=new Point();
		Point finger=new Point();
		List<Point> buffer=new LinkedList<Point>();
		List<Point> bufferfingers=new LinkedList<Point>();
		List<Point> fingers=new LinkedList<Point>();
		long temp=0;


		while(true && !close){

			if(!webcam.isOpened() && !close){
				System.out.println("Camera Error");
			}
			else{
				List<Point> defects=new LinkedList<Point>();
				if(!close){
					temp=System.currentTimeMillis();
					webcam.retrieve(mimm);
					//modification = v.filtromorfologico(2, 7, v.filtrocolorergb(0, 0, 0, 40, 40, 40, mimm));
					modification = v.morphologicalFilter(2, 7, v.colorFilterInHSV(0, 0, 0, 180, 255, 40,mimm));

					defects=v.envelopeDefects(mimm,v.searchForContours(mimm, modification, false, false, 450), false, 5);

					if(buffer.size()<7){
						buffer.add(v.centerOfPalm(mimm,defects));
					}
					else
					{
						center=v.filterAverageMovement(buffer, v.centerOfPalm(mimm,defects));
						//System.out.println((int)center.x+"         "+(int)center.y+"       "+(int)v.centerOfPalm(mimm,defects).x+"        "+(int)v.centerOfPalm(mimm,defects).y);
					}

					fingers=v.fingers(mimm, v.listOfContours(modification, 200), center);

					if(fingers.size()==1 && bufferfingers.size()<5){
						bufferfingers.add(fingers.get(0));
						finger=fingers.get(0);
					}
					else
					{
						if(fingers.size()==1){
							finger=v.filterAverageMovement(bufferfingers, fingers.get(0)); 
							//System.out.println((int)finger.x +"           "+(int)finger.y+"           "+(int)fingers.get(0).x+"           "+(int)fingers.get(0).y);
						}
					}

					v.drawFingersToCenterOfPalm(mimm, center, finger, fingers);



					v.mousetrack(fingers,finger,center, r,true,mimm,temp);

					v.frametolabel(mimm);

				}
			}

		}


	}
}


