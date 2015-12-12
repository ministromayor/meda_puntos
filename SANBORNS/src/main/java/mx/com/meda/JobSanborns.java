package mx.com.meda;

import org.apache.log4j.Logger;
import javax.ejb.Schedule;
import javax.ejb.Singleton;


@Singleton(name = "Sanborns")
public class JobSanborns {

	Logger log = Logger.getLogger(this.getClass());

	@Schedule(second="0", minute="0", hour="17", persistent=false)
	public void entrada() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.SANBORNS);
		proc.procesarEntrada();
		proc.release();
	}

	@Schedule(second="0", minute="0", hour="8", persistent=false)
	public void salida() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.SANBORNS);
		proc.procesarSalida();
		proc.release();
	}



}
