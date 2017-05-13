package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.loginservercon.LSConnection;
import ru.l2gw.gameserver.loginservercon.gspackets.SendAccountInfoUpdate;
import ru.l2gw.gameserver.model.CharSelectInfoPackage;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.playerSubOrders.Vitality;
import ru.l2gw.gameserver.network.GameClient;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.tables.CharTemplateTable;
import ru.l2gw.gameserver.templates.L2PlayerTemplate;
import ru.l2gw.util.AutoBan;
import ru.l2gw.util.Location;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class CharacterSelectionInfo extends L2GameServerPacket
{
	// d (SdSddddddddddffdQdddddddddddddddddddddddddddddddddddddddffdddchhd)
	private String _loginName;
	private int _sessionId;
	private CharSelectInfoPackage[] _characterPackages;

	public CharacterSelectionInfo(String loginName, int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
	}

	public CharSelectInfoPackage[] getCharInfo()
	{
		return _characterPackages;
	}

	@Override
	public void runImpl()
	{
		getClient().charLoaded = false;
	}

	@Override
	protected final void writeImpl()
	{
		int size = _characterPackages != null ? _characterPackages.length : 0;

		writeC(0x09);
		writeD(size);
		writeD(0x07); // Максимальное количество персонажей на сервере
		writeC(0x00); // 0x00 - Разрешить, 0x01 - запретить. Разрешает или запрещает создание игроков
		writeC(0x02); // 0x01 - Выводит окно, что нужно купить игру, что создавать более 2х чаров. 0х02 - обычное лобби.
		writeD(0x00);
		writeC(0x00); // 0x01 - Предлогает купить ПА.

		long lastAccess = -1L;
		int lastUsed = -1;
		for(int i = 0; i < size; i++)
			if(lastAccess < _characterPackages[i].getLastAccess())
			{
				lastAccess = _characterPackages[i].getLastAccess();
				lastUsed = i;
			}
		for(int i = 0; i < size; i++)
		{
			CharSelectInfoPackage charInfoPackage = _characterPackages[i];

			writeS(charInfoPackage.getName());
			writeD(charInfoPackage.getCharId()); // ?
			writeS(_loginName);
			writeD(_sessionId);
			writeD(charInfoPackage.getClanId());
			writeD(0x00); // ??

			writeD(charInfoPackage.getSex());
			writeD(charInfoPackage.getRace());
			writeD(charInfoPackage.getBaseClassId());

			writeD(0x01); // default active for all (GOD)

			writeD(charInfoPackage.getX());
			writeD(charInfoPackage.getY());
			writeD(charInfoPackage.getZ());

			writeF(charInfoPackage.getCurrentHp());
			writeF(charInfoPackage.getCurrentMp());

			writeD(charInfoPackage.getSp());
			writeQ(charInfoPackage.getExp());
			int lvl = charInfoPackage.getLevel();
			writeF(Experience.getExpPercent(lvl, charInfoPackage.getExp()));
			writeD(lvl);

			writeD(charInfoPackage.getKarma());
			writeD(charInfoPackage.getPkKills());
			writeD(charInfoPackage.getPvP());

			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			
			for(int PAPERDOLL_ID : Inventory.PAPERDOLL_ORDER)
				writeD(charInfoPackage.getPaperdollItemId(PAPERDOLL_ID));

			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_RHAND)); //Внешний вид оружия (ИД Итема).
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_LHAND)); //Внешний вид щита (ИД Итема).
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_GLOVES)); //Внешний вид перчаток (ИД Итема).
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_CHEST)); //Внешний вид верха (ИД Итема).
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_LEGS)); //Внешний вид низа (ИД Итема).
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_FEET)); //Внешний вид ботинок (ИД Итема).
			writeD(0x00); //???
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_HAIR)); //Внешний вид шляпы (ИД итема).
			writeD(charInfoPackage.getPaperdollVisualId(Inventory.PAPERDOLL_DHAIR)); //Внешний вид маски (ИД итема).
			
			writeD(charInfoPackage.getHairStyle());
			writeD(charInfoPackage.getHairColor());
			writeD(charInfoPackage.getFace());

			writeF(charInfoPackage.getMaxHp()); // hp max
			writeF(charInfoPackage.getMaxMp()); // mp max

			writeD(charInfoPackage.getAccessLevel() > -100 ? charInfoPackage.getDeleteTimer() : -1);
			writeD(charInfoPackage.getClassId());
			writeD(i == lastUsed ? 1 : 0);
			
			writeC(Math.min(charInfoPackage.getPaperdollEnchantEffect(Inventory.PAPERDOLL_RHAND), 127));
			writeH(charInfoPackage.getPaperdollVariation1Id(Inventory.PAPERDOLL_RHAND));
			writeH(charInfoPackage.getPaperdollVariation2Id(Inventory.PAPERDOLL_RHAND));
			int weaponId = charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND);
			if(weaponId == 8190) // Transform id (на оффе отображаются только КВ трансформации или вообще не отображаются ;)
				writeD(301);
			else if(weaponId == 8689)
				writeD(302);
			else
				writeD(0x00);
			
			//TODO: Pet info?
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeF(0x00);
			writeF(0x00);

			writeD(charInfoPackage.getVitalityPoints());
			/*TODO: Сделать, чтобы реагировало на ПА.
			if(hasPremiumAccount)
			{
				writeD(charInfoPackage.getVitalityPoints() > 0 ? (int) (100 * Config.ALT_VITALITY_PA_RATE) : 100);
				writeD(Config.ALT_VITALITY_POTIONS_PA_LIMIT - charInfoPackage.getVitalityUsedPotions());
			}
			else
			{*/
				writeD(charInfoPackage.getVitalityPoints() > 0 ? (int) (100 * Config.VIT_RATE) : 100);
				writeD(Config.VIT_MAX_POINTS - charInfoPackage.getVitalityPoints());
			/*}*/
			writeD(charInfoPackage.getAccessLevel() > -100 ? 0x01 : 0x00); // Access level
			writeC(0x00);
			writeC(0x00);
		}
	}

	private CharSelectInfoPackage[] loadCharacterSelectInfo()
	{
		CharSelectInfoPackage charInfopackage;
		ArrayList<CharSelectInfoPackage> characterList = new ArrayList<CharSelectInfoPackage>();

		Connection con = null;
		Statement statement = null;
		Statement statement2 = null;
		ResultSet pl_rset = null;
		ResultSet ps_rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			statement2 = con.createStatement();
			pl_rset = statement.executeQuery("SELECT * FROM `characters` WHERE `account_name`='" + _loginName + "' LIMIT 7");
			byte deleted = 0;

			while(pl_rset.next())// fills the package
			{
				ps_rset = statement2.executeQuery("SELECT * FROM `character_subclasses` WHERE `char_obj_id`='" + pl_rset.getInt("obj_Id") + "' AND `isBase`='1' LIMIT 1");
				if(!ps_rset.next())
					throw new Exception("Missing base subclass for player " + pl_rset.getString("char_name")+" objectId "+pl_rset.getInt("obj_Id")+" login: "+_loginName);

				L2PlayerTemplate templ = CharTemplateTable.getInstance().getTemplate(ps_rset.getInt("class_id"), pl_rset.getInt("sex") == 1);
				if(templ == null)
				{
					_log.warn("Warning! no char template for classId: " + ps_rset.getInt("class_id") + " sex: " + pl_rset.getInt("sex"));
					continue;
				}
				int raceId = templ.race.ordinal();
				int baseClassId = ps_rset.getInt("class_id");

				ps_rset = statement2.executeQuery("SELECT * FROM `character_subclasses` WHERE `char_obj_id`='" + pl_rset.getInt("obj_Id") + "' AND `active`='1' LIMIT 1");
				if(!ps_rset.next())
					throw new Exception("Missing active subclass for player " + pl_rset.getString("char_name"));

				charInfopackage = restoreChar(pl_rset, ps_rset);

				if(charInfopackage != null)
				{
					charInfopackage.setRace(raceId);
					charInfopackage.setBaseClassId(baseClassId);
					if(charInfopackage.getDeleteTimer() > 0 || charInfopackage.getAccessLevel() < 0)
						deleted++;
					characterList.add(charInfopackage);
				}
			}
			LSConnection.getInstance().sendPacket(new SendAccountInfoUpdate(_loginName, (byte) characterList.size(), deleted));
		}
		catch(Exception e)
		{
			_log.warn("could not restore charinfo:", e);
		}
		finally
		{
			DbUtils.closeQuietly(statement2, ps_rset);
			DbUtils.closeQuietly(con, statement, pl_rset);
		}

		return characterList.toArray(new CharSelectInfoPackage[characterList.size()]);
	}

	private CharSelectInfoPackage restoreChar(ResultSet chardata, ResultSet charclass)
	{
		CharSelectInfoPackage charInfopackage = null;
		try
		{
			int classid = charclass.getInt("class_id");
			boolean female = chardata.getInt("sex") == 1;
			int objectId = chardata.getInt("obj_Id");
			String name = chardata.getString("char_name");
			charInfopackage = new CharSelectInfoPackage(objectId, name);
			charInfopackage.setLevel(charclass.getInt("level"));
			charInfopackage.setMaxHp(charclass.getInt("maxHp"));
			charInfopackage.setCurrentHp(charclass.getDouble("curHp"));
			charInfopackage.setMaxMp(charclass.getInt("maxMp"));
			charInfopackage.setCurrentMp(charclass.getDouble("curMp"));

			charInfopackage.setFace(chardata.getInt("face"));
			charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
			charInfopackage.setHairColor(chardata.getInt("haircolor"));
			charInfopackage.setSex(female ? 1 : 0);

			charInfopackage.setExp(charclass.getLong("exp"));
			charInfopackage.setSp(charclass.getInt("sp"));
			charInfopackage.setClanId(chardata.getInt("clanid"));

			charInfopackage.setKarma(chardata.getInt("karma"));
			charInfopackage.setPkKills(chardata.getInt("pkkills"));
			charInfopackage.setPvP(chardata.getInt("pvpkills"));
			charInfopackage.setClassId(classid);
			charInfopackage.setLocation(new Location(chardata.getInt("x"), chardata.getInt("y"), chardata.getInt("z")));

			long deletetime = chardata.getLong("deletetime");
			int deletedays = 0;
			if(Config.DELETE_DAYS > 0)
				if(deletetime > 0)
				{
					deletetime = (int) (System.currentTimeMillis() / 1000 - deletetime);
					deletedays = (int) (deletetime / 3600 / 24);
					if(deletedays >= Config.DELETE_DAYS)
					{
						GameClient.deleteFromClan(objectId, charInfopackage.getClanId());
						GameClient.deleteCharByObjId(objectId);
						return null;
					}
					deletetime = Config.DELETE_DAYS * 3600 * 24 - deletetime;
				}
				else
					deletetime = 0;
			charInfopackage.setDeleteTimer((int) deletetime);
			charInfopackage.setLastAccess(chardata.getLong("lastAccess") * 1000);
			charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));
			long logoutTime = chardata.getLong("logoutTime");
			int vitalityPoints = (int) (((System.currentTimeMillis() / 1000 - logoutTime) / 60) * Vitality._pointsPerMin + chardata.getInt("vitPoints"));
			if(vitalityPoints > 20000)
				vitalityPoints = 20000;
			else if(vitalityPoints < 0)
				vitalityPoints = 0;
			charInfopackage.setVitalityPoints(vitalityPoints);

			if(charInfopackage.getAccessLevel() < 0 && !AutoBan.isBanned(objectId))
				charInfopackage.setAccessLevel(0);
		}
		catch(Exception e)
		{
			_log.warn(getClass().getSimpleName() + e);
			e.printStackTrace();
		}

		return charInfopackage;
	}
}
