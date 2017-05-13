package ru.l2gw.commons.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.ArrayUtils;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: rage
 * @date: 02.03.12 18:29
 */
public class ServerConfig
{
	private static final Pattern uc = Pattern.compile("\\_([A-Za-z0-9]{1})");

	public static void loadConfig(Class<?> configClass, String config)
	{
		try
		{
			Log log = LogFactory.getLog(configClass);
			ExProperties settings = new ExProperties();
			LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new FileInputStream("config/" + config + ".properties"), "UTF-8"));
			settings.load(lnr);
			lnr.close();

			try
			{
				Field propertiesField = configClass.getDeclaredField(config + "Properties");
				if(propertiesField != null)
					propertiesField.set(null, settings);
			}
			catch (Exception e)
			{
				// quite
			}

			for(Field field : configClass.getDeclaredFields())
			{
				ConfigField configField;
				if((configField = field.getAnnotation(ConfigField.class)) != null && config.equals(configField.config()))
				{
					String fieldName = configField.fieldName();

					if(fieldName == null || fieldName.isEmpty())
						fieldName = getNormalName(field.getName());

					try
					{
						if(field.getType().getSimpleName().equalsIgnoreCase("boolean"))
							field.setBoolean(null, settings.getBooleanProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("byte"))
							field.setByte(null, settings.getByteProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("int"))
							field.setInt(null, settings.getIntProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("long"))
							field.setLong(null, settings.getLongProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("float"))
							field.setFloat(null, settings.getFloatProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("double"))
							field.setDouble(null, settings.getDoubleProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("string"))
							field.set(null, settings.getProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("string[]"))
							field.set(null, settings.getArrayProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("int[]"))
							field.set(null, settings.getIntArrayProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("long[]"))
							field.set(null, settings.getLongArrayProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("double[]"))
							field.set(null, settings.getDoubleArrayProperty(fieldName, configField.value()));
						else if(field.getType().getSimpleName().equalsIgnoreCase("float[]"))
							field.set(null, settings.getFloatArrayProperty(fieldName, configField.value()));
						else
							log.info("Unknown field type: " + field.getType().getSimpleName() + " field name: " + field.getName() + " config: " + config + ".properties");
					}
					catch(NumberFormatException e)
					{
						e.printStackTrace();
						throw new Error("Failed to Load config/" + config + ".properties File. Field: " + field.getName() + " " + e.getMessage());
					}
					log.debug(config + ": set " + field.getName() + "{" + fieldName + "} = " + field.get(null));
				}
			}

			try
			{
				Method method = configClass.getMethod(config + "Custom", ExProperties.class);
				method.invoke(null, settings);
			}
			catch(NoSuchMethodException e)
			{
				// queite
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load config/" + config + ".properties File.");
		}
	}

	public static Object setField(Class<?> configClass, String fieldName, String value) throws Exception
	{
		Log log = LogFactory.getLog(configClass);
		for(Field field : configClass.getDeclaredFields())
		{
			ConfigField configField;
			if(field.getName().equalsIgnoreCase(fieldName) || ((configField = field.getAnnotation(ConfigField.class)) != null && fieldName.equalsIgnoreCase(configField.fieldName())) || getNormalName(field.getName()).equals(fieldName))
			{
				Object oldValue = field.get(null);
				if(field.getType().getSimpleName().equalsIgnoreCase("boolean"))
					field.setBoolean(null, Boolean.parseBoolean(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("byte"))
					field.setByte(null, Byte.parseByte(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("int"))
					field.setInt(null, Integer.parseInt(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("long"))
					field.setLong(null, Long.parseLong(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("float"))
					field.setFloat(null, Float.parseFloat(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("double"))
					field.setDouble(null, Double.parseDouble(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("string"))
					field.set(null, value);
				else if(field.getType().getSimpleName().equalsIgnoreCase("string[]"))
					field.set(null, ArrayUtils.toStringArray(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("int[]"))
					field.set(null, ArrayUtils.toIntArray(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("long[]"))
					field.set(null, ArrayUtils.toLongArray(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("double[]"))
					field.set(null, ArrayUtils.toDoubleArray(value));
				else if(field.getType().getSimpleName().equalsIgnoreCase("float[]"))
					field.set(null, ArrayUtils.toFloatArray(value));
				else
					log.info("Unknown field type: " + field.getType().getSimpleName() + " field name: " + field.getName());

				return oldValue;
			}
		}

		throw new NoSuchFieldException("Filed: " + fieldName + " not found in " + configClass.getSimpleName());
	}

	private static String getNormalName(String name)
	{
		name = name.toLowerCase();
		StringBuffer sb = new StringBuffer();
		Matcher m = uc.matcher(name);
		while(m.find())
		{
			m.appendReplacement(sb, m.group(0).replace("_" + m.group(1), m.group(1).toUpperCase()));
		}

		m.appendTail(sb);
		return sb.replace(0, 1, sb.toString().substring(0, 1).toUpperCase()).toString();
	}

	public static String showConfig(Class<?> configClass, String config)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Config: config/").append(config).append(".properties").append(" settings:\n");
		for(Field field : configClass.getDeclaredFields())
		{
			ConfigField configField;
			if((configField = field.getAnnotation(ConfigField.class)) != null && config.equals(configField.config()))
			{
				String fieldName = configField.fieldName();
				if(fieldName.isEmpty())
					fieldName = getNormalName(field.getName());
				try
				{
					sb.append(field.getName()).append("{").append(fieldName).append("}=").append(field.get(null)).append("\n");
				}
				catch(Exception e)
				{
					sb.append(field.getName()).append(" error: ").append(e).append("\n");
				}
			}
		}

		return sb.toString();
	}
}