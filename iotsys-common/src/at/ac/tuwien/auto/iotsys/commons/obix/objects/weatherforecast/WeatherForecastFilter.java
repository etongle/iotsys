package at.ac.tuwien.auto.iotsys.commons.obix.objects.weatherforecast;

import obix.*;

public interface WeatherForecastFilter extends IObj {

	public static final String CONTRACT = "iot:WeatherForecastFilter";
	
	public static final String limitContract = "<int name='limit' val='0' min='0' null='true'/>";
	public Int limit();

	public static final String startContract = "<abstime name='start' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
	public Abstime start();

	public static final String endContract = "<abstime name='end' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
	public Abstime end();
}
