package ru.l2gw.commons.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Random;
import java.util.StringTokenizer;

/**
 * @author: rage
 * @date: 03.03.12 16:01
 */
public class NetUtil
{
	protected static Log _log = LogFactory.getLog(NetUtil.class);

	public static boolean checkIfIpInRange(String ip, String ipRange)
	{
		//DATA FIELD
		//Ip format. 110.20.30.40 - 50.50.30.40
		int userIp1 = -1;
		int userIp2 = -1;
		int userIp3 = -1;
		int userIp4 = -1;
		String firstIp;
		String lastIp;
		//Data field end.

		ip = ip.replace(".", ",");
		for(String s : ip.split(","))
			if(userIp1 == -1)
				userIp1 = Integer.parseInt(s);
			else if(userIp2 == -1)
				userIp2 = Integer.parseInt(s);
			else if(userIp3 == -1)
				userIp3 = Integer.parseInt(s);
			else
				userIp4 = Integer.parseInt(s);

		int ipMin1 = -1;
		int ipMin2 = -1;
		int ipMin3 = -1;
		int ipMin4 = -1; //IP values for min ip
		int ipMax1 = -1;
		int ipMax2 = -1;
		int ipMax3 = -1;
		int ipMax4 = -1; //Ip values for max ip

		StringTokenizer st = new StringTokenizer(ipRange, "-"); // Try to split them by "-" symbol

		_log.debug("Tokens in string " + ipRange + ": " + st.countTokens());

		if(st.countTokens() == 2)
		{
			firstIp = st.nextToken(); //get our first ip string
			lastIp = st.nextToken(); //get out second ip string.

			firstIp = firstIp.replace(".", ",");
			lastIp = lastIp.replace(".", ",");

			//Set our minimum ip
			for(String s1 : firstIp.split(","))
				if(ipMin1 == -1)
					ipMin1 = Integer.parseInt(s1);
				else if(ipMin2 == -1)
					ipMin2 = Integer.parseInt(s1);
				else if(ipMin3 == -1)
					ipMin3 = Integer.parseInt(s1);
				else
					ipMin4 = Integer.parseInt(s1);

			//set our maximum ip
			for(String s2 : lastIp.split(","))
				if(ipMax1 == -1)
					ipMax1 = Integer.parseInt(s2);
				else if(ipMax2 == -1)
					ipMax2 = Integer.parseInt(s2);
				else if(ipMax3 == -1)
					ipMax3 = Integer.parseInt(s2);
				else
					ipMax4 = Integer.parseInt(s2);

			//Now we are making some checks with our ips.
			if(userIp1 > ipMin1 && userIp1 < ipMax1)
				return true; // it's internal
			else if(userIp1 < ipMin1 || userIp1 > ipMax1)
				return false; // it's external
			else if(userIp1 == ipMin1 && userIp1 != ipMax1)
			{
				if(userIp2 > ipMin2)
					return true;
				else if(userIp2 < ipMin2)
					return false;
				else if(userIp3 > ipMin3)
					return true;
				else if(userIp3 < ipMin3)
					return false;
				else
					return userIp4 >= ipMin4;
			}
			else if(userIp1 != ipMin1 && userIp1 == ipMax1)
			{
				if(userIp2 < ipMax2)
					return true;
				else if(userIp2 > ipMax2)
					return false;
				else if(userIp3 < ipMax3)
					return true;
				else if(userIp3 > ipMax3)
					return false;
				else
					return userIp4 <= ipMax4;
			}
			else if(userIp2 > ipMin2 && userIp2 < ipMax2)
				return true; // it's internal
			else if(userIp2 < ipMin2 || userIp2 > ipMax2)
				return false; // it's external
			else if(userIp2 == ipMin2 && userIp2 != ipMax2)
			{
				if(userIp3 > ipMin3)
					return true;
				else if(userIp3 < ipMin3)
					return false;
				else
					return userIp4 >= ipMin4;
			}
			else if(userIp2 != ipMin2 && userIp2 == ipMax2)
			{
				if(userIp3 < ipMax3)
					return true;
				else if(userIp3 > ipMax3)
					return false;
				else
					return userIp4 <= ipMax4;
			}
			else if(userIp3 > ipMin3 && userIp3 < ipMax3)
				return true; // it's internal
			else if(userIp3 < ipMin3 || userIp3 > ipMax3)
				return false; // it's external
			else if(userIp3 == ipMin3 && userIp3 != ipMax3)
				return userIp4 >= ipMin4;
			else if(userIp3 != ipMin3 && userIp3 == ipMax3)
				return userIp4 <= ipMax4;
			else if(userIp4 >= ipMin4 && userIp4 <= ipMax4)
				return true; // it's internal
			else if(userIp4 < ipMin4 || userIp4 > ipMax4)
				return false; // it's external
		}
		else if(st.countTokens() == 1)
		{
			if(ip.equalsIgnoreCase(ipRange))
				return true;
		}
		else
			_log.warn("Error in internal ip detection: " + ipRange);
		return false;
	}

	/**
	 * Получает длинну пакета из 2-х первых байт.<BR>
	 * Используется для общения между LS и GS.
	 * @param first первый байт пакета
	 * @param second второй байт пакета
	 * @return длинна пакета
	 */
	public static int getPacketLength(byte first, byte second)
	{
		int lenght = first & 0xff;
		return lenght |= second << 8 & 0xff00;
	}

	/**
	 * Дописывает длинну пакета.<BR>
	 * Используется для общения между LS и GS.
	 * @param data пакет для отправления (зашифрован уже)
	 * @return готовый пакет для отправления
	 */
	public static byte[] writeLenght(byte[] data)
	{
		int newLenght = data.length + 2;
		byte[] result = new byte[newLenght];
		result[0] = (byte) (newLenght & 0xFF);
		result[1] = (byte) (newLenght >> 8 & 0xFF);
		System.arraycopy(data, 0, result, 2, data.length);
		return result;
	}
}
