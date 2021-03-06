package cloud.distrFileSys.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

import cloud.distrFileSys.master.model.BackUpSessions;
import cloud.distrFileSys.master.model.FileReps;
import cloud.distrFileSys.master.model.Sessions;
import cloud.distrFileSys.support.service.CipherForFile;
import cloud.distrFileSys.support.service.Configuration;
import cloud.distrFileSys.support.service.FileApi;
import cloud.distrFileSys.support.service.StatisticsInputStream;
import retrofit.RestAdapter;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Test;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.Files.CommitInfo;
import com.dropbox.core.v2.Files.UploadBuilder;
import com.dropbox.core.v2.Files.UploadSessionAppendBuilder;
import com.dropbox.core.v2.Files.UploadSessionCursor;
import com.dropbox.core.v2.Files.UploadSessionFinishBuilder;
import com.dropbox.core.v2.Files.UploadSessionFinishUploader;
import com.dropbox.core.v2.Files.UploadSessionStartUploader;
import com.dropbox.core.v2.Files.WriteMode;
import com.google.common.io.ByteStreams;
public class ClientSideTest {
	private static final String SERVER = "http://localhost:8080/SpringServer/";
	
	
	private File uploadingFData;

	private cloud.distrFileSys.master.model.File file;
	
	private FileApi fileSvc = new RestAdapter.Builder()
	.setEndpoint(SERVER).build()
	.create(FileApi.class);
	
	
	
	public File getUploadingFData() {
		return uploadingFData;
	}


	public void setUploadingFData(String path) {
		this.uploadingFData = new File(path);
	}


	public cloud.distrFileSys.master.model.File getFile() {
		return file;
	}


	public void setFile(cloud.distrFileSys.master.model.File file) {
		this.file = file;
	}
	
	public void setFile(String type,Long size,String path,String name,Long uId){
		this.file=cloud.distrFileSys.master.model.File.create().
				withFileType(type).withName(name).withPath(path).withSize(size).withUserId(uId).build();
	
	}
	
	private Sessions sess;
	private Long UID=new Long(1);
	private String PATH="src/test/resources/test.mp4";
	private Long testOffSet=new Long(0);
	@Test
	public void initialUpload() throws Exception{
		String path;Long uId;
		path=PATH;
		uId=UID;
		
		File newF=new File(path);
		Path p=Paths.get(newF.getAbsolutePath());
		try {
			BasicFileAttributes attr
			= Files.getFileAttributeView(p, BasicFileAttributeView.class)
			       .readAttributes();
			
			Long size= new Long(attr.size());
			String name=newF.getName();
			String sfix=name.substring(name.lastIndexOf(".")+1);
			
			setFile(sfix,size,path,name,uId);
			
			Sessions s=fileSvc.addFile(uId, file);
			
			System.out.println(s.getError());
			
			assertEquals(s.getError(), "0");
			
			this.sess=s;
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//return s;
	}
	
	@Test
	public void finishUpload() throws Exception{
		initialUpload();
		String session=this.sess.getSession();
		Integer i=fileSvc.addFileE(this.UID, session,testOffSet,file);
		assertTrue(i==1);
		System.out.println(i);	
	}
	
	@Test
	public void upload() throws Exception{
		
		String path=PATH;
		Long uid=UID;
		initialUpload();
		System.out.println("-----------------------------------------init end-----------------------------");
		testOffSet=file.getSize();
		String session=this.sess.getSession();
		String accTok=this.sess.getAccessToken();
		DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
		DbxClientV2 client = new DbxClientV2(config, accTok);
//		UploadSessionStartUploader st=null;
//		InputStream in = new FileInputStream(path);
//		
//		st=client.files.uploadSessionStart();
//		client.files.uploadBuilder("/p").run(in);
//		UploadBuilder ub=client.files.uploadBuilder(path);
		
		
//		String sessionId=st.finish().sessionId;
//		this.sess.setSession(sessionId);
		UploadSessionAppendBuilder upAppBuilder=client.files.uploadSessionAppendBuilder(session, 0);
		InputStream fi=new FileInputStream(path);
		fi=CipherForFile.encrptionInputStream("squirrel123", fi);
		upAppBuilder.run(fi);
		System.out.println(testOffSet+"||"+fi.available());
//		Date dNow = new Date( );
//		SimpleDateFormat ft = new SimpleDateFormat ("E-yyyy-MM-dd-hh-mm-ss-a-zzz");
//		
//		String cloudPath=Configuration.PATH_IN_CLOUD+ft.format(dNow)+"/"+file.getName();
//		
//		
//		UploadSessionCursor cursor =new UploadSessionCursor(session,testOffSet);
//		
//		
//		CommitInfo commit=new CommitInfo(cloudPath,WriteMode.add,false,new Date(),false);
//		
//		UploadSessionFinishBuilder finisher=client.files.uploadSessionFinishBuilder(cursor, commit);
//		UploadSessionFinishUploader finishUpload=finisher.start();
//		System.out.print(finishUpload.finish().pathLower);
		System.out.println("-----------------------------------------fin start-----------------------------");
		//finishUpload();
		
		Long i=(long)(fileSvc.addFileE(uid, session,testOffSet,file));
		if(i<(long)0){
			testOffSet=0-i;
			i=(long)(fileSvc.addFileE(uid, session,testOffSet,file));
			System.out.println("uploading");
		}
		if(i==(long)0)
			System.out.println("upload failed");
		
		System.out.println(i);
		System.out.println("uploaded!");
		
//..........................................backup for file................................//
		if(!this.sess.getAccessToken_backup().equals("N/A")){
			DbxRequestConfig configb = new DbxRequestConfig("dropbox/java-tutorial", "en_US");
			DbxClientV2 clientb = new DbxClientV2(configb, this.sess.getAccessToken_backup());
			InputStream inb = new FileInputStream(path);
			inb=CipherForFile.encrptionInputStream("squirrel123", inb);
			Date dNow = new Date( );
			SimpleDateFormat ft = new SimpleDateFormat ("E-yyyy-MM-dd-hh-mm-ss-a-zzz");
			String back_path=Configuration.PATH_IN_CLOUD_BACKUP+ft.format(dNow)+"/"+file.getName();
			String Id_in_co=clientb.files.uploadBuilder(back_path).run(inb).id;
			BackUpSessions bs=new BackUpSessions();
			bs.setCemail(clientb.users.getCurrentAccount().email);
			bs.setCpath(back_path);
			bs.setFid(i);
			inb.close();
			fileSvc.addFileB(bs);
			
			
			
		}
		
		
		
	}
	
}
