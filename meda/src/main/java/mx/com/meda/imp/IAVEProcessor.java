package mx.com.meda.imp;

import org.apache.log4j.Logger;
import mx.com.meda.Processor;
import mx.com.meda.DataWrapper;
import mx.com.meda.SFTPClient;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.PushbackReader;
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

public class IAVEProcessor extends AliadoProcessor implements Processor {

	public IAVEProcessor() {
		super(Socio.IAVE);
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
				BufferedReader br = new BufferedReader(new InputStreamReader(cliente.readLastInFileLocalMode(file_name)));
				String linea = null;	

				boolean valid_trailer = false;
				while( (linea = br.readLine()) != null ) {
					log.debug(">>"+linea);
					String[] values = new String[in_campos+1];
					log.debug("Se separará la cadena con \""+in_separador+"\"");
					String[] tokens = linea.split(Pattern.quote(in_separador));
					values[0] = file_name;

					if( lines == 0 && in_header ) {
						log.debug("No se debe procesar el header para este tipo de aliado.");
					} else if(lines != 0 && !br.ready() && !hayDatosDisponibles(br) && in_trailer) {
						log.debug("Se procesará el trailer.");
						log.info("TRAILER: "+linea);
						valid_trailer = validarTrailer(tokens, lines);
					} else {
						if( tokens.length == in_campos ) {
							log.info(">>"+linea);
						} else {
							log.error("La linea ["+linea+"] mide "+linea.length()+" caracteres pero se esperaba que midiera "+in_campos);
						}
						if(tokens != null ) {
							log.debug("Se copiarán los tokens.");
							System.arraycopy(tokens, 0, values, 1, in_campos);
							log.debug("Se cargarán los valores copiados desde los tokens en la base de datos.");
							dw.cargarLinea(TipoDeArchivo.RECIBE_TICKETS.getId(), values);
							tokens = null;
							lines++;
						}
					}

				}
				if( valid_trailer && dw.procArchivoCarga(TipoDeArchivo.RECIBE_TICKETS.getId(), file_name) ) {
					cliente.backupInFile(file_name);	
					this.procesarSalida();
				} else {
					log.error("Ocurrió un error durante el proceso de entrada.");
					dw.limpiarRegistrosFallidos(file_name);
				}
				br.close();
			}
		} catch( SftpException ex ) {
			log.error("No se puedo procesar la entrada.");
			log.warn(ex.getMessage());
		} finally {
			cliente.desconectar();
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
		in_nombre = "IMD"+date+".acc";
		return in_nombre;
	}

	private String buildOutputFilename() {
		return out_nombre;
	}


	private boolean hayDatosDisponibles(BufferedReader br) {
		boolean flag = false;
		try {
			PushbackReader pbr = new PushbackReader(br);
			int tmp = pbr.read();
			if(tmp == -1) {
				log.debug("Se ha llegado al final del flujo.");
			} else {
				pbr.unread(tmp);
				log.debug("No se ha llegado al final de flujo.");
				flag = true;	
			}
		} catch(IOException ex) {
			log.error("No se pudo determinar si el flujo ha finalizado.");
			log.warn(ex.getMessage());
		} finally {
			return flag;
		}
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

	public boolean workarround() {
		return true;
	}

}