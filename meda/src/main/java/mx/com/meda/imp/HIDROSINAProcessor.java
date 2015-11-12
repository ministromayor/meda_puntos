package mx.com.meda.imp;

import org.apache.log4j.Logger;
import mx.com.meda.Processor;
import mx.com.meda.DataWrapper;
import mx.com.meda.SFTPClient;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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

public class HIDROSINAProcessor extends AliadoProcessor implements Processor {

	public HIDROSINAProcessor() {
		super(Socio.HIDROSINA);
		log = Logger.getLogger(this.getClass());
	}

	public boolean procesarEntrada() {
		log.debug("Se comenzará la lectura del archivo de entrada.");
		try {
			if( cliente.conectar() )  {
				String file_name = cliente.lastAddedInFileName();
				log.info("Se cargará el achivo: "+file_name);
				BufferedReader br = new BufferedReader(new InputStreamReader(cliente.readLastInFile()));
				String linea = null;	
				while( (linea = br.readLine()) != null ) {
					log.debug(linea);
					String[] values = new String[in_campos+1];
					values[0] = file_name;
					log.debug("Se separará la cadena con \""+in_separador+"\"");
					String[] tokens = linea.split(Pattern.quote(in_separador));
					if(tokens.length != in_campos) {
						log.error("La linea contiene "+tokens.length+" elementos pero se esperaba que tuviera "+in_campos);
					} else {
						System.arraycopy(tokens, 0, values, 1, in_campos);
						dw.cargarLinea(1, values);
					}
				}
				if( dw.procArchivoCarga(1, file_name) ) {
					this.procesarSalida();
				} else {
					log.error("No se procesará salida debido a que ocurrió un error durante el proceso de entrada.");
				}
				br.close();
				cliente.desconectar();
			}
		} catch( SftpException ex ) {
			ex.printStackTrace();
		} finally {
			return true;
		}
	}

	public boolean procesarSalida() { 
		log.debug("No se genera archivo de salida para OSTAR");
		return true;
	}

}