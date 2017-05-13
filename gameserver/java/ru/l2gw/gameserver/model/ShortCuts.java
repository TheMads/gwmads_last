package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExAutoSoulShot;
import ru.l2gw.gameserver.serverpackets.ShortCutInit;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ShortCuts
{
	private static Log _log = LogFactory.getLog(ShortCuts.class.getName());

	private L2Player _owner;
	private ConcurrentHashMap<Integer, L2ShortCut> _shortCuts = new ConcurrentHashMap<Integer, L2ShortCut>();

	public ShortCuts(L2Player owner)
	{
		_owner = owner;
	}

	public Collection<L2ShortCut> getAllShortCuts()
	{
		return _shortCuts.values();
	}

	public L2ShortCut getShortCut(int slot, int page)
	{
		L2ShortCut sc = _shortCuts.get(slot + page * 12);
		// verify shortcut
		if(sc != null && sc.type == L2ShortCut.TYPE_ITEM)
			if(_owner.getInventory().getItemByObjectId(sc.id) == null)
			{
				_owner.sendPacket(new SystemMessage(SystemMessage.THERE_ARE_NO_MORE_ITEMS_IN_THE_SHORTCUT));
				deleteShortCut(sc.slot, sc.page);
				sc = null;
			}
		return sc;
	}

	public void registerShortCut(L2ShortCut shortcut)
	{
		L2ShortCut oldShortCut = _shortCuts.put(shortcut.slot + 12 * shortcut.page, shortcut);
		registerShortCutInDb(shortcut, oldShortCut);
	}

	private synchronized void registerShortCutInDb(L2ShortCut shortcut, L2ShortCut oldShortCut)
	{
		if(oldShortCut != null)
			deleteShortCutFromDb(oldShortCut);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("REPLACE INTO character_shortcuts SET char_obj_id=?,slot=?,page=?,type=?,shortcut_id=?,level=?,class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.slot);
			statement.setInt(3, shortcut.page);
			statement.setInt(4, shortcut.type);
			statement.setInt(5, shortcut.id);
			statement.setInt(6, shortcut.level);
			statement.setInt(7, _owner.getActiveClass());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not store shortcuts:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * @param shortcut
	 */
	private void deleteShortCutFromDb(L2ShortCut shortcut)
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=? AND slot=? AND page=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.slot);
			statement.setInt(3, shortcut.page);
			statement.setInt(4, _owner.getActiveClass());
			statement.execute();
		}
		catch(Exception e)
		{
			_log.warn("could not delete shortcuts:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	/**
	 * Удаляет ярлык с пользовательской панели по номеру страницы и слота.
	 * @param slot
	 * @param page
	 */
	public void deleteShortCut(int slot, int page)
	{
		L2ShortCut old = _shortCuts.remove(slot + page * 12);
		if(old == null)
			return;
		deleteShortCutFromDb(old);
		// При удалении с панели скила, на оффе шлется полный инит ярлыков
		// Обработка удаления предметных ярлыков - клиент сайд.
		if(old.type == L2ShortCut.TYPE_SKILL)
			_owner.sendPacket(new ShortCutInit(_owner));
		if(old.type == L2ShortCut.TYPE_ITEM)
		{
			L2ItemInstance item = _owner.getInventory().getItemByObjectId(old.id);
			if(item != null && item.getItemType() == EtcItemType.SHOT)
			{
				_owner.removeAutoSoulShot(item.getItemId());
				_owner.sendPacket(new ExAutoSoulShot(item.getItemId(), false));
			}
		}
		for(int shotId : _owner.getAutoSoulShot())
			_owner.sendPacket(new ExAutoSoulShot(shotId, true));
	}

	/**
	 * Удаляет ярлык предмета с пользовательской панели.
	 * @param objectId
	 */
	public void deleteShortCutByObjectId(int objectId)
	{
		for(L2ShortCut shortcut : _shortCuts.values())
			if(shortcut != null && shortcut.type == L2ShortCut.TYPE_ITEM && shortcut.id == objectId)
				deleteShortCut(shortcut.slot, shortcut.page);
	}

	/**
	 * Удаляет ярлык скила с пользовательской панели.
	 * @param skillId
	 */
	public void deleteShortCutBySkillId(int skillId)
	{
		for(L2ShortCut shortcut : _shortCuts.values())
			if(shortcut != null && shortcut.type == L2ShortCut.TYPE_SKILL && shortcut.id == skillId)
				deleteShortCut(shortcut.slot, shortcut.page);
	}

	public void restore()
	{
		_shortCuts.clear();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT char_obj_id, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE char_obj_id=? AND class_index=?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, _owner.getActiveClass());
			rset = statement.executeQuery();
			while(rset.next())
			{
				int slot = rset.getInt("slot");
				int page = rset.getInt("page");
				int type = rset.getInt("type");
				int id = rset.getInt("shortcut_id");
				int level = rset.getInt("level");

				L2ShortCut sc = new L2ShortCut(slot, page, type, id, level);
				_shortCuts.put(slot + page * 12, sc);
			}
		}
		catch(Exception e)
		{
			_log.warn("could not store shortcuts:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}

		// Проверка ярлыков
		for(L2ShortCut sc : _shortCuts.values())
			// Удаляем ярлыки на предметы, которых нету в инвентаре
			if(sc.type == L2ShortCut.TYPE_ITEM)
			{
				if(_owner.getInventory().getItemByObjectId(sc.id) == null)
					deleteShortCut(sc.slot, sc.page);
			}
	}
}