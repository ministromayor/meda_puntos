package mx.com.meda.imp;

import org.apache.log4j.Logger;
import mx.com.meda.Processor;
import mx.com.meda.DataWrapper;
import mx.com.meda.SFTPClient;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.regex.Pattern;

import java.util.TimeZone;
import java.util.Date;
import java.util.List;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.JSchException;

import mx.com.meda.Socio;
import mx.com.meda.TipoDeArchivo;

public class OSTARProcessor extends AliadoProcessor implements Processor {

	public OSTARProcessor() {
		super(Socio.OSTAR);
		log = Logger.getLogger(this.getClass());
	}

	public boolean procesarEntrada() {
		log.debug("Se comenzará la lectura del archivo de entrada.");
		int lines = 0;
		String[] trailer = null;
		try {
			if( cliente.conectar() )  {
				String file_name = cliente.lastAddedInFileName(buildInputFilename());
				log.info("Se cargará el achivo: "+file_name);
				BufferedReader br = new BufferedReader(new InputStreamReader(cliente.readLastInFile(file_name)));
				String linea = null;	
				while( (linea = br.readLine()) != null ) {
					log.info(">>"+linea);
					String[] values = new String[in_campos+1];
					values[0] = file_name;
					log.debug("Se separará la cadena con \""+in_separador+"\"");
					String[] tokens = linea.split(Pattern.quote(in_separador));

					if(((tokens.length != in_campos) && br.ready()) ||
						((!br.ready() && !in_trailer) && (tokens.length != in_campos))) {
						log.error("La linea ["+linea+"] contiene "+tokens.length+" elementos pero se esperaba que tuviera "+in_campos);
						tokens = null;
					} else if(!br.ready() && in_trailer) {
						log.info("Se considerará la cadena "+linea+" como trailer.");
						trailer = tokens;
						tokens = null;
					} 
					if(tokens != null ) {
						System.arraycopy(tokens, 0, values, 1, in_campos);
						dw.cargarLinea(TipoDeArchivo.RECIBE_ACREDITACIONES.getId(), values);
						lines++;
					}
				}
				if( validarTrailer(trailer, lines) && dw.procArchivoCarga(TipoDeArchivo.RECIBE_ACREDITACIONES.getId(), file_name) ) {
					this.procesarSalida();
				} else {
					log.error("No se procesará salida debido a que ocurrió un error durante el proceso de entrada.");
				}
				br.close();
				cliente.desconectar();
			}
		} catch( SftpException ex ) {
			log.error("No se puedo procesar la entrada.");
			log.warn(ex.getMessage());
		} finally {
			return true;
		}
	}

	public boolean procesarSalida() {
		return true;
	}

	private String buildInputFilename() {
		String date_format = "ddMMyyyy";
		DateFormat df = new SimpleDateFormat(date_format);
		df.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
		String date = df.format(new Date());
		in_nombre = "OST"+date+".acc";
		return in_nombre;
	}

	private String buildOutputFilename() {
		return out_nombre;
	}

	private boolean validarTrailer(String[] tokens, int registros) {
		boolean flag = false;
		if(in_trailer) {
			int lineas_recibidas = Integer.valueOf(tokens[0]).intValue();
			if((tokens.length == in_t_campos) && (lineas_recibidas == registros)) {
				flag = true;
			} else {
				flag = false;
			}
			log.info("El trailer es: "+ (flag ? "Valido" : "Erroneo") +" para "+registros+" registros.");
		} else {
			flag = true;
		}
		return flag;
	}

}