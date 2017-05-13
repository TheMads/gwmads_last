package ru.l2gw.gameserver.model.base;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.tables.OptionData;

import java.sql.Connection;
import java.sql.PreparedStatement;

public final class L2Augmentation
{
	private static final Log _log = LogFactory.getLog(L2Augmentation.class);

	private L2ItemInstance item;
	private final int effectsId;
	private final EnchantOption option1, option2;
	private boolean active;
	private final int mineralId;

	public L2Augmentation(L2ItemInstance item, int effects, int mineral, boolean save)
	{
		this.item = item;
		effectsId = effects;
		option1 = OptionData.getEnchantOption(0x0000FFFF & effectsId);
		option2 = OptionData.getEnchantOption(effectsId >> 16);
		mineralId = mineral;
		active = false;

		if(save)
			saveAugmentationData();
	}

	/**
	 * Get the augmentation "id" used in serverpackets.
	 * @return augmentationId
	 */
	public int getAugmentationId()
	{
		return effectsId;
	}

	public int getMineralId()
	{
		return mineralId;
	}

	/**
	 * Applys the bonus to the player.
	 * @param player
	 */
	public void applyBonus(L2Player player)
	{
		if(active)
			return;

		active = true;

		Env env = new Env(player, null, null);

		boolean skill = false;

		if(option1 != null)
		{
			for(FuncTemplate ft : option1.getFunctions())
			{
				player.addStatFunc(ft.getFunc(env, this));
			}

			if(option1.getSkill() != null)
			{
				skill = true;
				player.addSkill(option1.getSkill(), false);
			}
		}

		if(option2 != null)
		{
			for(FuncTemplate ft : option2.getFunctions())
			{
				player.addStatFunc(ft.getFunc(env, this));
			}

			if(option2.getSkill() != null)
			{
				skill = true;
				player.addSkill(option2.getSkill(), false);
			}
		}

		if(skill)
		{
			player.sendPacket(new SkillList(player));
		}
	}

	/**
	 * Removes the augmentation bonus from the player.
	 * @param player
	 */
	public void removeBonus(L2Player player)
	{
		if(!active)
			return;

		active = false;

		player.removeStatsOwner(this);

		boolean skill = false;
		if(option1.getSkill() != null)
		{
			skill = true;
			player.removeSkill(option1.getSkill(), false);
		}

		if(option2.getSkill() != null)
		{
			skill = true;
			player.removeSkill(option2.getSkill(), false);
		}

		if(skill)
		{
			player.sendPacket(new SkillList(player));
		}
	}

	public void setItem(L2ItemInstance newItem)
	{
		if(newItem == null)
			return;

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("UPDATE augmentations SET item_id=? WHERE item_id=?");
			statement.setInt(1, newItem.getObjectId());
			statement.setInt(2, item.getObjectId());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn("Could not save augmentation for item: " + newItem + " from DB:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
		item = newItem;
	}

	private void saveAugmentationData()
	{
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			statement = con.prepareStatement("INSERT INTO augmentations (item_id,attributes,mineral) VALUES (?,?,?)");
			statement.setInt(1, item.getObjectId());
			statement.setInt(2, effectsId);
			statement.setInt(3, mineralId);

			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn("Could not save augmentation for item: " + item + " from DB:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public void deleteAugmentationData()
	{
		if(!item.isAugmented())
			return;

		// delete the augmentation from the database
		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM augmentations WHERE item_id=?");
			statement.setInt(1, item.getObjectId());
			statement.executeUpdate();
		}
		catch(Exception e)
		{
			_log.warn("Could not delete augmentation for item: " + item + " from DB:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}
}