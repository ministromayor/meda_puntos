package mx.com.meda;

public interface Processor {

	public boolean procesarEntrada();
	public boolean procesarSalida();
	public void release();

}