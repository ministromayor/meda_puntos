package mx.com.meda;

import org.apache.log4j.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpATTRS;

import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.io.InputStream;
import java.io.IOException;

public class SFTPClient {

	private String 	cfg_host 	= "127.0.0.1";
	private String 	cfg_path 	= "./Entrada";
	private int 		cfg_port 	= 22;
	private String 	cfg_usuario = "guest";
	private String 	cfg_password = "guest";
	private int 		cfg_timeout = 15000;
	private String 	cfg_in_dir	= "/";
	private String 	cfg_out_dir	= "/";
	private String		cfg_known_hosts = "/etc/meda/known_hosts";

	private static final String MEDA_SFTP_PROPERTIES_FILENAME = "sftp-client.properties";
	private Logger log = Logger.getLogger(this.getClass());


	private JSch jsch = null;
	private Session session = null;
	private ChannelSftp sftp = null;

	public SFTPClient(Socio peer) throws JSchException {
		try {
			loadConfiguration(peer.getNombre());
		}catch(IOException ex) {
			log.error("No se pudo cargar el archivo de configuración, se utilizarán los ajustes por defecto.");
		}
		jsch = new JSch();
		jsch.setKnownHosts(cfg_known_hosts);
	}

	private void loadConfiguration(String peerName) throws IOException {
		Properties settings = new Properties();
		settings.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(MEDA_SFTP_PROPERTIES_FILENAME));

		String lc_peerName = peerName.toLowerCase();
		String[] peer_properties_keys = new String[]{
			lc_peerName + ".host",
			lc_peerName + ".port",
			lc_peerName + ".user",
			lc_peerName + ".pasw",
			lc_peerName + ".timeout",
			lc_peerName + ".in",
			lc_peerName + ".out"
		};

		String val = null;

		val = settings.getProperty(lc_peerName + ".host");
		cfg_host = val != null ? val : cfg_host;

		val = settings.getProperty(lc_peerName + ".port");
		cfg_port = val != null ? Integer.valueOf(val).intValue() : cfg_port;

		val = settings.getProperty(lc_peerName + ".user");
		cfg_usuario = val != null ? val : cfg_usuario;

		val = settings.getProperty(lc_peerName + ".pasw");
		cfg_password = val != null ? val : cfg_password;

		val = settings.getProperty(lc_peerName + ".timeout");
		cfg_timeout = val != null ? Integer.valueOf(val).intValue() : cfg_timeout;

		val = settings.getProperty(lc_peerName + ".in");
		cfg_in_dir = val != null ? val : cfg_in_dir;

		val = settings.getProperty(lc_peerName + ".out");
		cfg_out_dir = val != null ? val : cfg_out_dir;

		String[] global_properties_keys = new String[]{
			"global.known_hosts"
		};
		cfg_known_hosts = settings.getProperty(global_properties_keys[0]);

		for(String propiedad : peer_properties_keys) {
			log.debug("propiedad "+propiedad+": "+settings.getProperty(propiedad));
		}
	}
	
	public boolean conectar() throws JSchException {
		session = jsch.getSession(cfg_usuario, cfg_host, cfg_port);
		session.setPassword(cfg_password.getBytes());
		session.setTimeout(cfg_timeout);
		session.connect();
		log.debug("Ahora la sesión se encuentra " + (session.isConnected() ? "conectada" : "desconectada"));
		sftp = (ChannelSftp)session.openChannel("sftp");
		if(sftp != null) { 
			sftp.connect();
		}
		return (session.isConnected() & sftp.isConnected());
	}


	public String lastAddedInFileName() throws SftpException {
		return lastAddedFileName(cfg_in_dir, "*");
	}

	public String lastAddedInFileName(String name) throws SftpException {
		return lastAddedFileName(cfg_in_dir, name);
	}

	private String lastAddedFileName(String rootRelativeDir, String file_name) throws SftpException {
		String fileName = "";
		Date recentDate = new Date(0L);
		if(sftp.isConnected()) {
			if(!sftp.pwd().equalsIgnoreCase(rootRelativeDir)) 
				sftp.cd("/"+rootRelativeDir);
			Vector<ChannelSftp.LsEntry> l_archivos = sftp.ls(file_name);
			for(ChannelSftp.LsEntry entrada : l_archivos) {
				SftpATTRS atrs = entrada.getAttrs();
				Date cf_date = new Date((long)atrs.getMTime() * 1000);
				if(cf_date.after(recentDate)) { 
					recentDate = cf_date; 
					fileName = entrada.getFilename();
				}
			}
			log.debug("El archivo "+fileName+" es el mas reciente en el directorio "+rootRelativeDir);
			return fileName;
		} else {
			log.error("El canal sftp está cerrado.");
			return "";	
		}
	}

	public InputStream readLastInFile() throws SftpException {
		return readLastInFile("*");
	}

	public InputStream readLastInFile(String name) throws SftpException {
		InputStream is = null;
		String lf = this.lastAddedFileName(cfg_in_dir, name);
		log.debug("Se entregará un flujo de entrada para el archivo "+lf);
		is = sftp.get(lf);
		return is;
	}

	public void uploadOutFile(InputStream data, String name) throws SftpException {
		String path = this.cfg_out_dir+"/"+name;
		log.info("Se subirá el archivo: "+path);
		sftp.put(data, path);
	}
	
	protected void listFiles() throws SftpException {
		if(sftp.isConnected()) {
			if(!sftp.pwd().equalsIgnoreCase(cfg_path)) 
				sftp.cd(cfg_path);
			Vector<ChannelSftp.LsEntry> l_archivos = sftp.ls("*");
			for(ChannelSftp.LsEntry entrada : l_archivos) {
				SftpATTRS atrs = entrada.getAttrs();
				long epoch_mtime = (long)atrs.getMTime() * 1000;
				
				String fecha = null;
				try {
					DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
					df.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
					fecha = df.format(new Date(epoch_mtime));
				} catch( NullPointerException ex ) { 
					log.error("No se pudo procesar la fecha del archivo: "+entrada.getFilename());
					fecha = new Date().toString();
				}
				log.info("Nombre: "+entrada.getFilename()+"\tFecha de modificación: "+ fecha + "\tepoch millis: " + String.valueOf(epoch_mtime));
			}
		}
	}

	public boolean desconectar() {
		sftp.disconnect();
		session.disconnect();
		return true;
	}

}