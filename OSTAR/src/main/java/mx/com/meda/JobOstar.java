package mx.com.meda;

import org.apache.log4j.Logger;
import javax.ejb.Schedule;
import javax.ejb.Singleton;


@Singleton(name = "Ostar")
public class JobOstar {

	Logger log = Logger.getLogger(this.getClass());

	@Schedule(second="0", minute="15", hour="1", persistent=false)
	public void ejecutar() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.OSTAR);
		proc.procesarEntrada();
		proc.release();
	}

}
