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

@Singleton(name = "JobHidrosina")
public class JobHidrosina {

	Logger log = Logger.getLogger(this.getClass());
	@Schedule(second="0", minute="30", hour="2", persistent=false)
	public void hidrosina() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.HIDROSINA);
		proc.procesarEntrada();
		proc.release();
	}

}
