package mx.com.meda;

import org.apache.log4j.Logger;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ejb.Schedule;
import javax.ejb.Singleton;

import java.sql.SQLException;

import java.io.IOException;
import java.io.InputStream;
import com.jcraft.jsch.JSchException;

import mx.com.meda.imp.SanbornsProcessor;


@Singleton(name = "JobChedraui")
public class JobChedraui {

	Logger log = Logger.getLogger(this.getClass());
	@Schedule(second="30", minute="*/10", hour="*", persistent=false)
	public void entrada() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.CHEDRAUI);
		proc.procesarEntrada();
		proc.release();
	}

	@Schedule(second="0", minute="*/10", hour="*", persistent=false)
	public void salida() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.CHEDRAUI);
		proc.procesarSalida();
		proc.release();
	}

}
