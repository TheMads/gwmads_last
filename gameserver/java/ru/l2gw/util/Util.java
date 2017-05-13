package ru.l2gw.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.serverpackets.PlaySound;
import ru.l2gw.gameserver.serverpackets.ShortCutRegister;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.gameserver.tables.TerritoryTable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util
{
	protected static Log _log = LogFactory.getLog(Util.class.getName());

	static final String PATTERN = "0.0000000000E00";
	static final DecimalFormat df;

	/**
	 * Форматтер для адены.<br>
	 * Locale.KOREA заставляет его фортматировать через ",".<br>
	 * Locale.FRANCE форматирует через " "<br>
	 * Для форматирования через "." убрать с аргументов Locale.FRANCE
	 */
	private static NumberFormat adenaFormatter;

	static
	{
		adenaFormatter = NumberFormat.getIntegerInstance(Locale.ENGLISH);
		df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
		df.applyPattern(PATTERN);
		df.setPositivePrefix("+");
	}

	public static String printData(byte[] data, int len)
	{
		StringBuffer result = new StringBuffer();

		int counter = 0;

		for(int i = 0; i < len; i++)
		{
			if(counter % 16 == 0)
				result.append(fillHex(i, 4) + ": ");

			result.append(fillHex(data[i] & 0xff, 2) + " ");
			counter++;
			if(counter == 16)
			{
				result.append("   ");

				int charpoint = i - 15;
				for(int a = 0; a < 16; a++)
				{
					int t1 = data[charpoint++];
					if(t1 > 0x1f && t1 < 0x80)
						result.append((char) t1);
					else
						result.append('.');
				}

				result.append("\n");
				counter = 0;
			}
		}

		int rest = data.length % 16;
		if(rest > 0)
		{
			for(int i = 0; i < 17 - rest; i++)
				result.append("   ");

			int charpoint = data.length - rest;
			for(int a = 0; a < rest; a++)
			{
				int t1 = data[charpoint++];
				if(t1 > 0x1f && t1 < 0x80)
					result.append((char) t1);
				else
					result.append('.');
			}

			result.append("\n");
		}

		return result.toString();
	}

	public static String fillHex(int data, int digits)
	{
		String number = Integer.toHexString(data);

		for(int i = number.length(); i < digits; i++)
			number = "0" + number;

		return number;
	}

	/**
	 * @param raw
	 * @return
	 */
	public static String printData(byte[] raw)
	{
		return printData(raw, raw.length);
	}

	/**
	 * Returns current timestamp in seconds (without milliseconds). Returned timestamp
	 * is obtained with the following expression: <p>
	 * <p/>
	 * <code>(System.currentTimeMillis() + 500L) / 1000L</code>
	 *
	 * @return Current timestamp
	 */
	public static long getTime()
	{
		return (System.currentTimeMillis() + 500L) / 1000L;
	}

	public static String formatDouble(double x, String nanString, boolean forceExponents)
	{
		if(Double.isNaN(x))
			return nanString;
		if(forceExponents)
			return df.format(x);
		if((long) x == x)
			return String.valueOf((long) x);
		return String.valueOf(x);
	}

	public static void handleIllegalPlayerAction(L2Player actor, String etc_str1, String etc_str2, int isBug)
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new IllegalPlayerAction(actor, etc_str1, etc_str2, isBug), 500);
	}

	public static String getRelativePath(File base, File file)
	{
		return file.toURI().getPath().substring(base.toURI().getPath().length());
	}

	/** Return degree value of object 2 to the horizontal line with object 1 being the origin */
	public static double calculateAngleFrom(L2Object obj1, L2Object obj2)
	{
		return calculateAngleFrom(obj1.getX(), obj1.getY(), obj2.getX(), obj2.getY());
	}

	/** Return degree value of object 2 to the horizontal line with object 1 being the origin */
	public static double calculateAngleFrom(int obj1X, int obj1Y, int obj2X, int obj2Y)
	{
		double angleTarget = Math.toDegrees(Math.atan2(obj1Y - obj2Y, obj1X - obj2X));
		if(angleTarget <= 0)
			angleTarget += 360;
		return angleTarget;
	}

	public static boolean checkIfInRange(int range, int x1, int y1, int x2, int y2)
	{
		return checkIfInRange(range, x1, y1, 0, x2, y2, 0, false);
	}

	public static boolean checkIfInRange(int range, int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		int dx = x1 - x2;
		int dy = y1 - y2;

		if(includeZAxis)
		{
			int dz = z1 - z2;
			return dx * dx + dy * dy + dz * dz <= range * range;
		}
		return dx * dx + dy * dy <= range * range;
	}

	public static boolean checkIfInRange(int range, L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		return !(obj1 == null || obj2 == null) && checkIfInRange(range, obj1.getX(), obj1.getY(), obj1.getZ(), obj2.getX(), obj2.getY(), obj2.getZ(), includeZAxis);
	}

	public static double convertHeadingToDegree(int heading)
	{
		double angle = 360.0 * heading / 65535.0;
		if(angle == 0)
			angle = 360;
		return angle;
	}

	public static double calculateDistance(int x1, int y1, @SuppressWarnings("unused") int z1, int x2, int y2)
	{
		return calculateDistance(x1, y1, 0, x2, y2, 0, false);
	}

	public static double calculateDistance(int x1, int y1, int z1, int x2, int y2, int z2, boolean includeZAxis)
	{
		double dx = x1 - x2;
		double dy = y1 - y2;

		if(includeZAxis)
		{
			int dz = z1 - z2;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static double calculateDistance(L2Object obj1, L2Object obj2, boolean includeZAxis)
	{
		if(obj1 == null || obj2 == null)
			return 1000000;
		return calculateDistance(obj1.getX(), obj1.getY(), obj1.getZ(), obj2.getX(), obj2.getY(), obj2.getZ(), includeZAxis);
	}

	public static short getShort(byte[] bs, int offset)
	{
		return (short) (bs[offset + 1] << 8 | bs[offset] & 0xff);
	}

	/**
	 * Пересекает ли отрезок p1p2 горизонтальный отрезок (x1,ys):(x2,ys)
	 *
	 * @param p1 один конец отрезка
	 * @param p2 другой конец отрезка
	 * @param x1 один конец горизонтали
	 * @param x2 другой конец горизонтали
	 * @return true если пересекает
	 */
	public static boolean isIntersectHorizontal(Location p1, Location p2, Location intersection, int x1, int x2)
	{
		// если отрезок целиком лежит с одной стороны от горизонтали
		if((p1.getX() - intersection.getY()) * (p2.getY() - intersection.getY()) > 0)
			return false;

		// х-координата точки пересечения отрезков
		// приведение к long для того, чтобы в конечном условии при умножении не было переполнения
		intersection.setX(p2.getX() - (intersection.getY() - p2.getY()) * (p2.getX() - p1.getX()) / (p1.getY() - p2.getY()));

		// точка пересечения должна лежать всредине обоих отрезков
		return ((long) intersection.getX() - p1.getX()) * (intersection.getX() - p2.getX()) <= 0 && ((long) intersection.getX() - x1) * (intersection.getX() - x2) <= 0;
	}

	/**
	 * Пересекает ли отрезок p1p2 вертикальный отрезок (xs,y1):(xs,y1)
	 *
	 * @param p1 один конец отрезка
	 * @param p2 другой конец отрезка
	 * @return true если пересекает
	 */
	public static boolean isIntersectVertical(Location p1, Location p2, Location intersection, int y1, int y2)
	{
		if((p1.getX() - intersection.getX()) * (p2.getX() - intersection.getX()) > 0)
			return false;
		intersection.setY(p2.getY() - (intersection.getX() - p2.getX()) * (p2.getY() - p1.getY()) / (p1.getX() - p2.getX()));
		return ((long) intersection.getY() - p1.getY()) * (intersection.getY() - p2.getY()) <= 0 && ((long) intersection.getY() - y1) * (intersection.getY() - y2) <= 0;
	}

	public static int getIntersectionX(Location p1, Location p2, int ys, @SuppressWarnings("unused") int x1, @SuppressWarnings("unused") int x2)
	{
		return p2.getX() - (ys - p2.getY()) * (p2.getX() - p1.getX()) / (p1.getY() - p2.getY());
	}

	public static int getIntersectionY(Location p1, Location p2, int xs, @SuppressWarnings("unused") int y1, @SuppressWarnings("unused") int y2)
	{
		return p2.getY() - (xs - p2.getX()) * (p2.getY() - p1.getY()) / (p1.getX() - p2.getX());
	}

	public static double getDistance(int x1, int y1, int x2, int y2)
	{
		return Math.hypot(x1 - x2, y1 - y2);
	}

	/**
	* Return amount of adena formatted with " " delimiter
	* @param amount
	* @return String formatted adena amount
	*/
	public static String formatAdena(long amount)
	{
		/*
		String s = "";
		int rem = amount % 1000;
		s = Integer.toString(rem);
		amount = (amount - rem) / 1000;
		while(amount > 0)
		{
			if(rem < 99)
				s = '0' + s;
			if(rem < 9)
				s = '0' + s;
			rem = amount % 1000;
			s = Integer.toString(rem) + " " + s;
			amount = (amount - rem) / 1000;
		}
		return s;
		*/
		return adenaFormatter.format(amount);
	}

	/**
	 * форматирует время в секундах в дни/часы/минуты/секунды
	 */
	public static String formatTime(long time)
	{
		String ret = "";
		long numDays = time / 86400;
		time -= numDays * 86400;
		long numHours = time / 3600;
		time -= numHours * 3600;
		long numMins = time / 60;
		time -= numMins * 60;
		long numSeconds = time;
		if(numDays > 0)
			ret += numDays + "d ";
		if(numHours > 0)
			ret += numHours + "h ";
		if(numMins > 0)
			ret += numMins + "m ";
		if(numSeconds > 0)
			ret += numSeconds + "s";
		return ret.trim();
	}

	/**
	 * Инструмент для подсчета выпавших вещей с учетом рейтов.
	 * Возвращает 0 если шанс не прошел либо количество если прошел.
	 * Корректно обрабатывает шансы превышающие 100%.
	 * Шанс в 1:1000000 (L2Drop.MAX_CHANCE)
	 */
	public static int rollDrop(int min, int max, double calcChance)
	{
		if(calcChance <= 0 || min <= 0 || max <= 0)
			return 0;
		int dropmult = 1;
		calcChance *= Config.RATE_DROP_ITEMS;
		if(calcChance > L2Drop.MAX_CHANCE)
		{
			if((int) Math.ceil(calcChance / L2Drop.MAX_CHANCE) <= calcChance / L2Drop.MAX_CHANCE)
				calcChance = Math.nextUp(calcChance);
			dropmult = (int) Math.ceil(calcChance / L2Drop.MAX_CHANCE);
			calcChance = calcChance / dropmult;
		}
		return Rnd.chance(calcChance) ? Rnd.get(min * dropmult, max * dropmult) : 0;
	}

	/**
	 * This method works the same as Thread.dumpStack(), but the only difference is that String with
	 * Stack Trace is beeing returned and nothing is printed into console.
	 * @return Stack Trace
	 */
	@SuppressWarnings( { "ThrowableInstanceNeverThrown" })
	public static String dumpStack()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		Exception e = new Exception("Stack trace");
		e.printStackTrace(pw);
		pw.flush();
		pw.close();
		return new String(baos.toByteArray());
	}

	public static Location correctCollision(int sx, int sy, Location dst, int collision)
	{
		return correctCollision(sx, sy, dst.getX(), dst.getY(), dst.getZ(), collision);
	}

	public static Location correctCollision(int sx, int sy, int tx, int ty, int tz, int collision)
	{

		if(tx == sx)
		{
			if(sy > ty)
				return new Location(tx, ty - collision, tz);
			else
				return new Location(tx, ty + collision, tz);
		}
		if(ty == sy)
		{
			if(sx > tx)
				return new Location(tx - collision, ty, tz);
			else
				return new Location(tx + collision, ty, tz);
		}

		int dx = tx - sx;
		int dy = ty - sy;

		double d = dx / Math.sqrt(dx * dx + dy * dy);

		int x = (int) (tx + d * collision);
		int y = dy * (x - sx) / dx + sy;
		return new Location(x, y, tz);
	}

	public static Location getPointInRadius(Location a, Location b, double angle)
	{
		double rad = Math.toRadians(angle + calculateAngleFrom(a.getX(), a.getY(), b.getX(), b.getY()));
		int r = (int) Math.sqrt((a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY()));
		return new Location((int) Math.round(b.getX() + Math.cos(rad) * r), (int) Math.round(b.getY() + Math.sin(rad) * r), b.getZ());
	}

	public static Location getPointInRadius(Location a, int radius, double angle)
	{
		double rad = Math.toRadians(angle);
		return new Location((int) Math.round(a.getX() + Math.cos(rad) * radius), (int) Math.round(a.getY() + Math.sin(rad) * radius), a.getZ());
	}

	public static int calculateCameraAngle(int heading)
	{
		double angle = 360.0 * heading / 65535;

		angle = 180 - angle;

		if(angle < 0)
			angle += 360;

		return (int)angle;
	}

	public static int calculateCameraAngle(L2NpcInstance target)
	{
		return calculateCameraAngle(target.getHeading());
	}

	public static int GetCharIDbyName(String name)
	{
		int res = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT `obj_Id` FROM `characters` WHERE `char_name` LIKE ? LIMIT 1");
			statement.setString(1, name);
			rset = statement.executeQuery();

			if(rset.next())
				res = rset.getInt("obj_Id");
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

	public static int getCharIdByNameAndName(String[] name)
	{
		int res = 0;

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT `obj_Id`,`char_name` FROM `characters` WHERE `char_name` LIKE ? LIMIT 1");
			statement.setString(1, name[0]);
			rset = statement.executeQuery();

			if(rset.next())
			{
				res = rset.getInt("obj_Id");
				name[0] = rset.getString("char_name");
			}
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

	public static int[] getCharLevelAndClassById(int objectId)
	{
		int res[] = new int[2];

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM `character_subclasses` WHERE `char_obj_id`=? AND `active`=1 LIMIT 1");
			statement.setInt(1, objectId);
			rset = statement.executeQuery();

			if(rset.next())
			{
				res[0] = rset.getInt("level");
				res[1] = rset.getInt("class_id");
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return res;
	}

	public static boolean checkBlockList(int ownerId, int targetId)
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT `target_id` FROM `character_blocklist` WHERE `obj_id` = ? and target_id = ?");
			statement.setInt(1, ownerId);
			statement.setInt(2, targetId);
			rset = statement.executeQuery();

			if(rset.next())
				return true;
		}
		catch(Exception e)
		{}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		return false;
	}

	public static String LastErrorConvertion(Integer LastError)
	{
		return LastError.toString();
	}

	public static int doXORdecGG(byte[] data, int data_len)
	{
		int ecx = 0;
		int pos = 0;
		int edx = 0;
		ecx = bytesToInt(data, 0);
		intToBytes( bytesToInt(data, 0) ^ ecx, data, 0);
		pos = data_len - 4;
		while(pos >= 0)
		{
			edx = bytesToInt(data, pos);
			edx ^= ecx;
			ecx -= edx;
			intToBytes(edx, data, pos);
			pos -= 4;
		}
		return ecx;
	}

	public static boolean verifyChecksum(byte[] raw, final int offset, final int size)
	{
		// check if size is multiple of 4 and if there is more then only the checksum
		if((size & 3) != 0 || size <= 4)
			return false;

  		long chksum = 0;
		int count = size - 4;
		int i = 0;
		for(i = offset; i < count; i += 4)
			chksum ^= bytesToInt(raw, i);

		long check = bytesToInt(raw, count);
		return check == chksum;
	}

	public static int bytesToInt(byte[] array, int offset)
	{
		return (((int) array[offset++] & 0xff) | (((int) array[offset++] & 0xff) << 8) | (((int) array[offset++] & 0xff) << 16) | (((int) array[offset++] & 0xff) << 24));
	}

	public static void intToBytes(int value, byte[] array, int offset)
	{
		array[offset++] = (byte) (value & 0xff);
		array[offset++] = (byte) (value >> 0x08 & 0xff);
		array[offset++] = (byte) (value >> 0x10 & 0xff);
		array[offset++] = (byte) (value >> 0x18 & 0xff);
	}

	public static Location convertVehicleCoordToWorld(Location vehicleWorldPos, Location inVehiclePos, boolean airShip)
	{
		double angle = convertHeadingToDegree(vehicleWorldPos.getHeading()) + (airShip ? 0 : 90);
		if(angle > 360)
			angle -= 360;
		double sinA = Math.sin(Math.toRadians(angle));
		double cosA = Math.cos(Math.toRadians(angle));
		int x = (int) Math.round(inVehiclePos.getX() * cosA - inVehiclePos.getY() * sinA + vehicleWorldPos.getX());
		int y = (int) Math.round(inVehiclePos.getX() * sinA + inVehiclePos.getY() * cosA + vehicleWorldPos.getY());
		int z = vehicleWorldPos.getZ() + inVehiclePos.getZ();
		//int z = Math.max(vehicleWorldPos.getZ() - 22, vehicleWorldPos.getZ() - inVehiclePos.getZ());
		return new Location(x, y, z);
	}

	public static Location convertWorldCoordToVehicle(Location vehicleWorldPos, Location worldPos, boolean airShip)
	{
		double angle = convertHeadingToDegree(vehicleWorldPos.getHeading()) + (airShip ? 0 : 90);
		int Xn = -vehicleWorldPos.getX() + worldPos.getX();
		int Yn = -vehicleWorldPos.getY() + worldPos.getY();
		int z = Math.max(vehicleWorldPos.getZ() - worldPos.getZ(), vehicleWorldPos.getZ() - 22);
		return getPointInRadius(new Location(Xn, Yn, 0), new Location(0, 0, z), -angle);
	}

	private static Pattern _pattern = Pattern.compile("<!--TEMPLET(\\d+)(.*?)TEMPLET-->", Pattern.DOTALL);

	public static HashMap<Integer, String> parseTemplate(String html)
	{
		Matcher m = _pattern.matcher(html);
		HashMap<Integer, String> tpls = new HashMap<Integer, String>();
		while(m.find())
		{
			tpls.put(Integer.parseInt(m.group(1)), m.group(2));
			html = html.replace(m.group(0), "");
		}

		tpls.put(0, html);
		return tpls;
	}

	public static String int2rgb(int color)
	{
		byte r = (byte) (color & 0xFF);
		byte g = (byte) ((color >> 8) & 0xFF);
		byte b = (byte) ((color >> 16) & 0xFF);

		return String.format("%02X%02X%02X", r, g, b);
	}

	public static int rgb2int(String color)
	{
		String r = color.substring(0, 2);
		String g = color.substring(2, 4);
		String b = color.substring(4, 6);
		return Integer.decode("0x" + b + g + r);
	}

	public static int pack2Int(short s1, short s2)
	{
		return (s1 << 16) | s2;
	}

	public static long pack2Long(int i1, int i2)
	{
		return ((long) i1 << 32) | i2;
	}

	public static long pack2Long(short s1, short s2, short s3, short s4)
	{
		return pack2Long(pack2Int(s1, s2), pack2Int(s3, s4));
	}

	public static short[] unpack2Short(long l1)
	{
		short[] ret = new short[4];
		ret[3] = (short) (l1 & 0xFFFF);
		ret[2] = (short) ((l1 >> 16) & 0xFFFF);
		ret[1] = (short) ((l1 >> 32) & 0xFFFF);
		ret[0] = (short) ((l1 >> 48) & 0xFFFF);
		return ret;
	}

	public static short[] unpack2Short(int i1)
	{
		short[] ret = new short[2];
		ret[1] = (short) i1;
		ret[0] = (short) (i1 >> 16);
		return ret;
	}

	public static int[] unpack2Int(long l1)
	{
		int[] ret = new int[2];
		ret[1] = (int) l1;
		ret[0] = (int) (l1 >> 32);
		return  ret;
	}

	public static boolean arrayContains(int[] array, int id)
	{
		for(int i : array)
			if(i == id)
				return true;
		return false;
	}

	public static int getTimeHour()
	{
		return GameTimeController.getInstance().getGameTime() / 60 % 24;
	}

	public static int getTimeMin()
	{
		return GameTimeController.getInstance().getGameTime() % 60;
	}

	public static int getMPCCId(L2Character cha)
	{
		if(cha == null)
			return 0;
		L2Player player = cha.getPlayer();
		if(player == null)
			return 0;
		L2Party party = player.getParty();
		if(party == null)
			return 0;
		L2CommandChannel cc = party.getCommandChannel();
		if(cc == null)
			return 0;
		return cc.getCommandChannelId();
	}

	public static int getPartyId(L2Character cha)
	{
		if(cha == null)
			return 0;
		L2Player player = cha.getPlayer();
		if(player == null)
			return 0;
		L2Party party = player.getParty();
		if(party == null)
			return 0;
		return party.getPartyId();
	}

	public static String intToFStr(int fString)
	{
		return "#" + fString;
	}

	public static void playSound(L2Player talker, String sound)
	{
		talker.sendPacket(new PlaySound(sound));
	}

	public static void rewardSkills(L2Player talker)
	{
		// Calculate the current higher Expertise of the L2Player
		for(short i = 0; i < L2Player.EXPERTISE_LEVELS.length; i++)
			if(talker.getLevel() >= L2Player.EXPERTISE_LEVELS[i])
				talker.expertiseIndex = i;

		// Add the Expertise skill corresponding to its Expertise level
		if(talker.expertiseIndex > 0)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(239, talker.expertiseIndex);
			talker.addSkill(skill, false);
		}

		boolean update = false;

		Map<Short, L2SkillLearn> availableSkills = SkillTreeTable.getInstance().getMaxEnableLevelsForSkillsAtLevel(talker, talker.getClassId());
		for(L2SkillLearn s : availableSkills.values())
		{
			L2Skill sk = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(sk == null)
			{
				_log.info("Warning: no skill id: " + s.getId() + " lvl: " + s.getLevel() + " " + talker);
				continue;
			}

			if(sk.getCanLearn(talker.getClassId()) && (talker.getKnownSkill(sk.getId()) == null || talker.getKnownSkill(sk.getId()).getLevel() < sk.getLevel()))
			{
				if(sk.isForgotten() && !Config.AUTO_LEARN_FORGOTTEN)
					continue;
				talker.addSkill(sk, true);
				if(talker.getAllShortCuts().size() > 0 && sk.getLevel() > 1)
					for(L2ShortCut sc : talker.getAllShortCuts())
						if(sc.id == sk.getId() && sc.type == L2ShortCut.TYPE_SKILL)
						{
							L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, sk.getLevel());
							talker.sendPacket(new ShortCutRegister(newsc));
							talker.registerShortCut(newsc);
						}
				update = true;
			}
		}

		if(update)
			talker.sendPacket(new SkillList(talker));

		// This function gets called on login, so not such a bad place to check weight
		// Update the overloaded status of the L2Player
		talker.refreshOverloaded();
		talker.refreshExpertisePenalty();
	}

	public static void completeQuest(L2Player talker, int questId)
	{
		Quest q = QuestManager.getQuest(questId);
		QuestState qs = q.newQuestState(talker);
		qs.exitCurrentQuest(false);
	}

	public static L2Party getParty(L2Character c0)
	{
		if(c0 == null)
			return null;

		L2Player p0 = c0.getPlayer();
		if(p0 == null)
			return null;

		return p0.getParty();
	}

	public static L2Party getPartyFromID(L2Character cha, int partyId)
	{
		if(cha == null || partyId < 1)
			return null;

		for(L2Player player : cha.getAroundPlayers(6000))
		{
			L2Party party = getParty(player);
			if(party != null && party.getPartyId() == partyId )
				return party;
		}

		return null;
	}

	public static int getCurrentTime()
	{
		return (int) (System.currentTimeMillis() / 1000);
	}

	public static L2Player getClanLeader(L2Character cha)
	{
		if(cha == null)
			return null;

		L2Player player = cha.getPlayer();
		if(player == null)
			return null;

		L2Clan clan = player.getClan();
		if(clan == null)
			return null;

		return clan.getLeader().getPlayer();
	}

	public static void teleportInMyTerritory(L2NpcInstance npc, int x, int y, int z, int radius)
	{
		if(npc == null)
			return;

		if(npc.getSpawnDefine() != null && npc.getSpawnDefine().getMaker().getTerritories().size() > 0)
		{
			for(L2Territory terr : npc.getSpawnDefine().getMaker().getTerritories())
			{
				for(L2Player player : L2ObjectsStorage.getAllPlayersForIterate())
				{
					if(player != null && player.getX() <= terr.getXmax() && player.getX() >= terr.getXmin() &&
							player.getY() <= terr.getYmax() && player.getY() >= terr.getYmin() &&
							player.getZ() <= terr.getZmax() && player.getZ() >= terr.getZmin() &&
							terr.isInside(player.getX(), player.getY(), player.getZ()))
					{
						player.teleToLocation(Location.coordsRandomize(x, y, z, 0, 0, radius));
					}
				}
			}
		}
		else if(npc.getSpawn() != null && npc.getSpawn().getLocation() > 0)
		{
			L2Territory terr = TerritoryTable.getInstance().getLocation(npc.getSpawn().getLocation());
			if(terr != null)
			{
				for(L2Player player : L2ObjectsStorage.getAllPlayersForIterate())
				{
					if(player != null && player.getX() <= terr.getXmax() && player.getX() >= terr.getXmin() &&
							player.getY() <= terr.getYmax() && player.getY() >= terr.getYmin() &&
							player.getZ() <= terr.getZmax() && player.getZ() >= terr.getZmin() &&
							terr.isInside(player.getX(), player.getY(), player.getZ()))
					{
						player.teleToLocation(Location.coordsRandomize(x, y, z, 0, 0, radius));
					}
				}
			}
		}
	}

	public static String md5(String encode)
	{
		if(StringUtils.isBlank(encode))
			return null;

		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] array = md.digest(encode.getBytes());
			StringBuilder sb = new StringBuilder();
			for(byte anArray : array)
				sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));

			return sb.toString();
		}
		catch(java.security.NoSuchAlgorithmException e)
		{
			// quite
		}
		return null;
	}
}