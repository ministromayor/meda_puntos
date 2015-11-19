package mx.com.meda.imp;

import org.apache.log4j.Logger;
import mx.com.meda.Processor;
import mx.com.meda.DataWrapper;
import mx.com.meda.SFTPClient;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.BufferedOutputStream;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.UUID;

import java.util.TimeZone;
import java.util.Date;
import java.util.List;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.JSchException;

import mx.com.meda.Socio;

public class AliadoProcessor {

	protected Logger log = null;
	Socio socio = null;
	private byte[] endline = "\n".getBytes();

	protected SFTPClient cliente;
	protected DataWrapper dw;

	private String tmp_file_id = null;
	private PrintWriter pw = null;

	protected String in_separador = "|";
	protected boolean in_header = false;
	protected boolean in_trailer = false;
	protected int in_campos = 14;
	protected int in_t_campos = 0;
	protected int in_h_campos = 0;
	protected String in_nombre = "default.acc";

	//Variables de configuración para los archivos de salida.
	protected String out_separador = "|";
	protected boolean out_header = false;
	protected boolean out_trailer = false;
	protected int out_campos = 0;
	protected int out_t_campos = 0;
	protected int out_h_campos = 0;
	protected String out_nombre = "default.acc.out";

	protected String MEDA_PROPERTIES_FILENAME = "default.properties";

	private void loadConfiguration() throws IOException {
		Properties settings = new Properties();
		settings.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(MEDA_PROPERTIES_FILENAME));

		in_separador = settings.getProperty("in.separador");

		in_trailer = new Boolean(settings.getProperty("in.trailer")).booleanValue();
		if(in_trailer)
			in_t_campos = new Integer(settings.getProperty("in.trailer.campos")).intValue();

		in_header = new Boolean(settings.getProperty("in.header")).booleanValue();
		if(in_header)
			in_h_campos = new Integer(settings.getProperty("in.header.campos")).intValue();
		in_campos = new Integer(settings.getProperty("in.campos")).intValue();
		in_nombre = settings.getProperty("in.nombre");

		out_separador = settings.getProperty("out.separador");
		out_header = new Boolean(settings.getProperty("out.header"));
		if(out_header) 
			out_h_campos = new Integer(settings.getProperty("out.header.campos")).intValue();
		out_trailer = new Boolean(settings.getProperty("out.trailer"));
		if(out_trailer)
			out_t_campos = new Integer(settings.getProperty("out.trailer.campos")).intValue();
		out_campos = new Integer(settings.getProperty("out.campos")).intValue();
		out_nombre = settings.getProperty("out.nombre");

	}

	public AliadoProcessor(Socio socio) {
		this.socio = socio;
		MEDA_PROPERTIES_FILENAME = socio.getNombre().toLowerCase()+".properties";
		try {
			loadConfiguration();
			this.cliente = new SFTPClient(socio);
			this.dw = new DataWrapper(socio);
		} catch (JSchException ex ) {
			log.error("Ocurrio un error al generar el cliente SFTP.");
			log.debug(ex.getMessage());
			//Manejar las excepciones futuras en el manejo de una clase instanciada con errores.
		} catch (IOException ex) {
			log.error("No se pudo cargar la configuración del módulo desde el archivo: "+MEDA_PROPERTIES_FILENAME);
			log.error(ex.getMessage());
		}
	}

	protected boolean escribirRespuesta(String linea) { 
		boolean flag = false;
		try {
			if(pw == null) {
				pw = new PrintWriter(new BufferedWriter(new FileWriter(getTempFile())));
				log.debug("Se ha inicializado un printwriter para el archivo de salida.");
			} 
			pw.println(linea);
			flag = true;
		} catch(IOException ex) {
			log.error("No se pudo escribir una linea en el flujo se salida.");
			log.error(ex.getMessage());
		} finally {
			return flag;
		}
	}

	protected InputStream recuperarRespuesta() {
		InputStream r_is = null;
		try {
			if(pw != null) {
				log.debug("Se limpiará el printWriter.");
				pw.flush();
				log.debug("Se cerrará el printWriter.");
				pw.close();
				pw = null;
				r_is = new FileInputStream(getTempFile());
			} else if(pw == null && tmp_file_id != null) {
				r_is = new FileInputStream(getTempFile());
			} else {
				log.warn("No hay un flujo de salida en el que se hubiera podido escribir una respuesta.");
			}
		} catch (IOException ex) {
			log.error("No se pudo finalizar el flujo de salida para alimentar totalmente la entrada del archivo de respuesta.");
			log.error(ex.getMessage());
		} finally {
			log.debug("Se devolverá un flujo de salida "+((r_is != null) ? "valido":"nulo"));
			return r_is;
		}
	}

	protected File getTempFile() {
		File tmp_file = null;
		if(tmp_file_id == null) {
			tmp_file_id = socio.getNombre().toLowerCase()+"_"+UUID.randomUUID().toString()+".tmp";
		}
		tmp_file = new File(File.separator+"tmp"+File.separator+tmp_file_id);
		log.debug("Se generará un archivo en la ruta: "+tmp_file.getAbsolutePath()+" para guardar temporalmente la salida de este proceso.");
		return tmp_file;
	}

	public void release() {
		dw.release();
	}

}