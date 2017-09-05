package application;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.mortennobel.imagescaling.ResampleOp;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class RootController {
	private String folderName;
	private ObservableList<ImgFileBean> fileData;
	Connection con;
	PreparedStatement pstmt;
	ResultSet rs;
	
	@FXML
	private Alert alert;
	@FXML
	private Button folderBtn, saveBtn;
	@FXML
	private TableView<ImgFileBean> imgTable;
	@FXML
	private TableColumn<ImgFileBean, String> nameCol;
	@FXML
	private Stage primaryStage;
	@FXML
	private File file;
	@FXML
	private Label folderPath;
	
	@FXML
	private void chooseFolder() throws IOException {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("사진 저장된 폴더 선택");
		file = dc.showDialog(primaryStage);
		if(file==null) {
			chooseFolder();
		}
		folderName = file.getPath();
		folderPath.setText(folderName);
		if(file.isDirectory()) {
			File[] list = file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith("jpg") || name.toLowerCase().endsWith("gif") 
							|| name.toLowerCase().endsWith("jpeg") || name.toLowerCase().endsWith("bmp")
							|| name.toLowerCase().endsWith("png");
				}
			});
			tableSet(list);
		}
	}

	private void tableSet(File[] list) {
		fileData = FXCollections.observableArrayList();
		ImgFileBean imgBean;
		for(int i=0;i<list.length;i++) {
			imgBean = new ImgFileBean();
			imgBean.setImgName(new SimpleStringProperty(list[i].getName()));
			fileData.add(imgBean);
		}
		nameCol.setCellValueFactory(fileData->fileData.getValue().getImgName());
		imgTable.setItems(fileData);
	}
	
	@FXML
	private void saveImg() throws FileNotFoundException, IOException, SQLException {
		for(int i=0;i<fileData.size();i++) {
			String imgFileName = fileData.get(i).getImgName().getValue();
			FileInputStream fis = new FileInputStream(folderName+"\\"+imgFileName);
			BufferedImage buffer = ImageIO.read(fis);
			String imgFile = imgFileName.substring(0,imgFileName.indexOf("."));
			if(imgFile.length()==5) {
				int classNum = Integer.parseInt(imgFileName.substring(0, 1));
				int banNum = Integer.parseInt(imgFileName.substring(1, 3));
				int numNum = Integer.parseInt(imgFileName.substring(3, 5));
				String st_id = getSt_id(classNum, banNum, numNum);
				if(st_id!="") {
					FileOutputStream fos = new FileOutputStream(folderName+"\\"+st_id+".jpg");
					fos.write(resize(buffer));
					fos.flush();
					fos.close();
					fis.close();
					File f = new File(folderName+"\\"+imgFileName);
					f.delete();
				}
			}
		}
	}
	
	private String getSt_id(int classNum, int banNum, int numNum) throws FileNotFoundException, IOException, SQLException {
		con = dbConn();
		String sql = "SELECT st_id FROM student  WHERE class="+classNum+" and ban="+banNum+" and num="+numNum;
		pstmt = con.prepareStatement(sql);
		rs =pstmt.executeQuery();
		String st_id = "";
		while(rs.next()) {
			st_id = rs.getString("st_id");
		}
		return st_id;
	}

	public static byte[] resize(BufferedImage src) throws IOException {
			int maxWidth = 300;
			double xyRatio = 1.3;
			//이미지 자를 포지션 설정
			int[] centerPoint = {src.getWidth() / 2, src.getHeight() / 2};
			
			//받은 이미지 사이즈
			int cropWidth = src.getWidth();
			int cropHeight = src.getHeight();
		    
			if (cropHeight > cropWidth * xyRatio) {
				cropHeight = (int) ( cropWidth * xyRatio );
		    } else {
		       	cropWidth = (int) ( (float) cropHeight / xyRatio );
		    }
			
			//저장될 이미지 사이즈
			int targetWidth = cropWidth;
			int targetHeight = cropHeight;
				
			if(targetWidth > maxWidth) {
				targetWidth = maxWidth;
				targetHeight = (int)(targetWidth * xyRatio);
			}
		       
		    BufferedImage destImg = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
		    Graphics2D g = destImg.createGraphics();
		    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		    g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		    g.drawImage(src, 0, 0, targetWidth, targetHeight, centerPoint[0] - (int)(cropWidth /2) , centerPoint[1] - (int)(cropHeight /2), centerPoint[0] + (int)(cropWidth /2), centerPoint[1] + (int)(cropHeight /2), null);
		    
		    byte[] returnByte = reSample(destImg, 300, 400, 300); 
		    return returnByte;
	}
	public static byte[] reSample (BufferedImage src, int width, int height, int dpi) throws IOException {
		ResampleOp resampleOp = new ResampleOp(width,height);
	    BufferedImage bsImg = resampleOp.filter(src, null);
	    
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    JPEGImageEncoder jpgEncoder = JPEGCodec.createJPEGEncoder(bos);
	    JPEGEncodeParam jpgParam = jpgEncoder.getDefaultJPEGEncodeParam(bsImg);
	    jpgParam.setDensityUnit(JPEGEncodeParam.DENSITY_UNIT_DOTS_INCH);
	    jpgEncoder.setJPEGEncodeParam(jpgParam);
	    jpgParam.setQuality(1, false);
	    jpgParam.setXDensity(dpi); jpgParam.setYDensity(dpi);
	    jpgEncoder.encode(bsImg, jpgParam);
	    
	    ImageIO.write(bsImg, "jpg", bos);
	    
	    return bos.toByteArray();
	}
	
	private Connection dbConn() throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader("C:/Uni_Cool/KCM_IP.DAT"));
		String ip = br.readLine();
		String license = br.readLine();
		br.close();
		String DBUrl="jdbc:sqlserver://"+ip+":1433;databaseName="+license;
		String user = "sa";
		String pwd = "unicool";
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			con = DriverManager.getConnection(DBUrl,user,pwd);
		} catch (Exception e) {
			e.printStackTrace();
			alert = new Alert(AlertType.ERROR);
			alert.setContentText("DB연결실패");
			alert.showAndWait();
		}
		return con;
	}
}
