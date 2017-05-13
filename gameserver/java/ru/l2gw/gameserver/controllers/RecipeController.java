package ru.l2gw.gameserver.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2ManufactureItem;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.entity.recipe.RecipeItem;
import ru.l2gw.gameserver.model.entity.recipe.RecipeList;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone.ZoneType;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2EtcItem.EtcItemType;
import ru.l2gw.gameserver.templates.L2Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;

public class RecipeController
{
	protected static Log _log = LogFactory.getLog(RecipeController.class.getName());

	private static final HashMap<Integer, RecipeList> _lists = new HashMap<Integer, RecipeList>();

	public static void load()
	{
		Connection con = null;
		PreparedStatement statement = null;
		PreparedStatement st2 = null;
		ResultSet list = null, rset2 = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM recipes");
			st2 = con.prepareStatement("SELECT * FROM `recitems` WHERE `recipe_id`=?");
			list = statement.executeQuery();

			while(list.next())
			{
				int recipeId = list.getInt("recipe_id");
				RecipeList recipeList = new RecipeList(recipeId, list.getInt("recipe_level"), list.getInt("recipe_item_id"), list.getInt("mp_consume"), list.getInt("success"), list.getInt("is_common"), list.getString("product"));
				st2.setInt(1, recipeId);
				rset2 = st2.executeQuery();
				while(rset2.next())
					recipeList.addMaterial(new RecipeItem(rset2.getInt("item_id"), rset2.getInt("count")));

				_lists.put(recipeId, recipeList);
			}

			_log.info("RecipeController: Loaded " + _lists.size() + " Recipes.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(st2, rset2);
			DbUtils.closeQuietly(con, statement, list);
		}
	}

	public static RecipeList getRecipeList(int listId)
	{
		return _lists.get(listId);
	}

	public static RecipeList getRecipeByItemId(int itemId)
	{
		for(RecipeList recipe : _lists.values())
			if(recipe.getRecipeItemId() == itemId)
				return recipe;

		return null;
	}

	@SuppressWarnings("unused")
	public static HashMap<Integer, RecipeList> getRecipesList()
	{
		return _lists;
	}

	public static void requestBookOpen(L2Player player, int isCommon)
	{
		if(isCommon == 0 && player.getSkillLevel(1321) < 1)
		{
			player.sendActionFailed();
			return;
		}
		
		RecipeBookItemList response = new RecipeBookItemList(isCommon, (int) player.getCurrentMp());
		if(isCommon == 0)
			response.setRecipes(player.getDwarvenRecipeBook());
		else
			response.setRecipes(player.getCommonRecipeBook());
		player.sendPacket(response);
	}

	public static void requestMakeItem(L2Player player, int recipeListId)
	{
		if(player.isInDuel())
		{
			player.sendPacket(Msg.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			return;
		}

		RecipeList recipeList = getRecipeList(recipeListId);
		player.resetWaitSitTime();

		if(recipeList == null || recipeList.getMaterials().size() == 0)
		{
			player.sendPacket(Msg.THE_RECIPE_IS_INCORRECT);
			return;
		}

		if(player.getCurrentMp() < recipeList.getMpConsume())
		{
			player.sendPacket(Msg.NOT_ENOUGH_MP);
			player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, -1));
			return;
		}

		if(!player.findRecipe(recipeListId))
		{
			player.sendPacket(Msg.PLEASE_REGISTER_A_RECIPE);
			player.sendActionFailed();
			return;
		}

		long totalLoad;
		RecipeItem product;

		synchronized(player.getInventory())
		{
			GArray<RecipeItem> materials = recipeList.getMaterials();
			Inventory inventory = player.getInventory();

			boolean itemsOk = true;
			for(RecipeItem recipeItem : materials)
			{
				if(recipeItem.quantity == 0)
					continue;

				if(Config.ALT_GAME_UNREGISTER_RECIPE && ItemTable.getInstance().getTemplate(recipeItem.itemId).getItemType() == EtcItemType.RECIPE)
				{
					RecipeList rp = getRecipeByItemId(recipeItem.itemId);
					if(player.findRecipe(rp))
						continue;
					player.sendPacket(Msg.NOT_ENOUGH_MATERIALS);
					player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, -1));
					return;
				}

				L2ItemInstance invItem = inventory.getItemByItemId(recipeItem.itemId);

				if(invItem == null || recipeItem.quantity > invItem.getCount())
				{
					long itemCount = invItem == null ? 0 : invItem.getCount();
					player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_MISSING_S2_S1_REQUIRED_TO_CREATE_THAT).addItemName(recipeItem.itemId).addNumber(recipeItem.quantity - itemCount));
					itemsOk = false;
				}
			}

			if(!itemsOk)
			{
				player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, -1));
				return;
			}

			product = recipeList.getProductItem();

			if(product == null)
			{
				_log.warn("RecipeController: product item is null for recipe: " + recipeListId);
				return;
			}

			totalLoad = ItemTable.getInstance().getTemplate(product.itemId).getWeight() * product.quantity;

			for(RecipeItem recipeItem : materials)
				if(recipeItem.quantity != 0)
					totalLoad -= ItemTable.getInstance().getTemplate(recipeItem.itemId).getWeight() * recipeItem.quantity;

			if(totalLoad > 0 && !player.getInventory().validateWeight(totalLoad))
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT));
				return;
			}

			player.reduceCurrentMp(recipeList.getMpConsume(), null);

			for(RecipeItem recipeItem : materials)
				if(recipeItem.quantity != 0)
				{
					if(Config.ALT_GAME_UNREGISTER_RECIPE && ItemTable.getInstance().getTemplate(recipeItem.itemId).getItemType() == EtcItemType.RECIPE)
						player.unregisterRecipe(getRecipeByItemId(recipeItem.itemId).getId());
					else
						player.destroyItemByItemId("Craft", recipeItem.itemId, recipeItem.quantity, player, true);
				}
		}

		if(!Rnd.chance(recipeList.getSuccessRate()))
			player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, 0));
		else
		{
			L2Item itemTemplate = ItemTable.getInstance().getTemplate(product.itemId);

			if(!itemTemplate.isStackable() && product.quantity > 1)
				for(int i = 0; i < product.quantity; i++)
					player.addItem("RecipeController", product.itemId, 1, player, true);
			else
				player.addItem("RecipeController", product.itemId, product.quantity, player, true);

			player.sendPacket(new RecipeItemMakeInfo(recipeList.getId(), player, 1));
		}

		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		su.addAttribute(StatusUpdate.CUR_MP, (int) player.getCurrentMp());
		player.sendPacket(su);
	}

	public static void requestManufactureItem(L2Player crafter, L2Player employer, int recipeListId)
	{
		RecipeList recipeList = getRecipeList(recipeListId);
		if(recipeList == null)
			return;

		crafter.resetWaitSitTime();

		if(recipeList.getMaterials().size() == 0)
			return;

		if(crafter.getCurrentMp() < recipeList.getMpConsume())
		{
			crafter.sendPacket(Msg.NOT_ENOUGH_MP);
			employer.sendPacket(Msg.NOT_ENOUGH_MP);
			employer.sendPacket(new RecipeShopItemInfo(crafter.getObjectId(), recipeListId, -1));
			return;
		}

		if(!crafter.findRecipe(recipeListId))
		{
			crafter.sendPacket(Msg.PLEASE_REGISTER_A_RECIPE);
			crafter.sendActionFailed();
			return;
		}

		long price = 0;

		for(L2ManufactureItem temp : crafter.getCreateList().getList())
			if(temp.getRecipeId() == recipeList.getId())
			{
				price = temp.getCost();
				break;
			}

		if(employer.getAdena() < price)
		{
			employer.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			employer.sendPacket(new RecipeShopItemInfo(crafter.getObjectId(), recipeListId, -1));
			return;
		}

		RecipeItem product = recipeList.getProductItem();

		if(product == null)
		{
			_log.warn("RecipeController: product item is null for recipe: " + recipeListId);
			return;
		}

		boolean success = Rnd.chance(recipeList.getSuccessRate());
		long totalLoad;

		synchronized(employer.getInventory())
		{
			GArray<RecipeItem> materials = recipeList.getMaterials();
			Inventory inventory = employer.getInventory();
			boolean itemsOk = true;

			for(RecipeItem recipeItem : materials)
			{
				if(recipeItem.quantity == 0)
					continue;

				L2ItemInstance invItem = inventory.getItemByItemId(recipeItem.itemId);

				if(invItem == null || recipeItem.quantity > invItem.getCount())
				{
					long itemCount = invItem == null ? 0 : invItem.getCount();
					employer.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_MISSING_S2_S1_REQUIRED_TO_CREATE_THAT).addItemName(recipeItem.itemId).addNumber(recipeItem.quantity - itemCount));
					itemsOk = false;
				}
			}

			if(!itemsOk)
			{
				employer.sendPacket(new RecipeShopItemInfo(crafter.getObjectId(), recipeListId, -1));
				return;
			}


			totalLoad = ItemTable.getInstance().getTemplate(product.itemId).getWeight() * product.quantity;

			for(RecipeItem recipeItem : materials)
				if(recipeItem.quantity != 0)
					totalLoad -= ItemTable.getInstance().getTemplate(recipeItem.itemId).getWeight() * recipeItem.quantity;

			if(totalLoad > 0 && !employer.getInventory().validateWeight(totalLoad))
			{
				employer.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT));
				return;
			}
			
			crafter.reduceCurrentMp(recipeList.getMpConsume(), null);

			if(!success)
			{
				SystemMessage sm;
				sm = new SystemMessage(SystemMessage.S1_HAS_FAILED_TO_CREATE_S2_AT_THE_PRICE_OF_S3_ADENA);
				sm.addString(crafter.getName());
				sm.addItemName(recipeList.getProductItemId());
				sm.addNumber(price);
				employer.sendPacket(sm);

				sm = new SystemMessage(SystemMessage.THE_ATTEMPT_TO_CREATE_S2_FOR_S1_AT_THE_PRICE_OF_S3_ADENA_HAS_FAILED);
				sm.addString(employer.getName());
				sm.addItemName(recipeList.getProductItemId());
				sm.addNumber(price);
				crafter.sendPacket(sm);
			}

			for(RecipeItem recipeItem : materials)
				if(recipeItem.quantity != 0)
					employer.destroyItemByItemId("Craft", recipeItem.itemId, recipeItem.quantity, crafter, true);
		}

		if(price > 0)
		{
			employer.reduceAdena("Craft", price, crafter, false);
			crafter.addAdena("Craft", price, employer, false);

			int tax = (int) (price * Config.SERVICES_TRADE_TAX / 100);
			if(crafter.isInZone(ZoneType.offshore))
				tax = (int) (price * Config.SERVICES_OFFSHORE_TRADE_TAX / 100);
			if(Config.SERVICES_TRADE_TAX_ONLY_OFFLINE && !crafter.isInOfflineMode())
				tax = 0;
			if(tax > 0)
			{
				crafter.reduceAdena("Craft", tax, employer, false);
				L2World.addTax(tax);
				crafter.sendMessage(new CustomMessage("trade.HavePaidTax", crafter).addNumber(tax));
			}
		}

		if(success)
		{
			L2Item itemTemplate = ItemTable.getInstance().getTemplate(product.itemId);

			SystemMessage sm;
			if(product.quantity > 1)
			{
				sm = new SystemMessage(SystemMessage.S1_CREATED_S2_S3_AT_THE_PRICE_OF_S4_ADENA);
				sm.addString(crafter.getName());
				sm.addNumber(product.quantity);
				sm.addItemName(product.itemId);
				sm.addNumber(price);
				employer.sendPacket(sm);

				sm = new SystemMessage(SystemMessage.S2_S3_HAVE_BEEN_CREATED_FOR_S1_AT_THE_PRICE_OF_S4_ADENA);
				sm.addString(employer.getName());
				sm.addNumber(product.quantity);
				sm.addItemName(product.itemId);
				sm.addNumber(price);
				crafter.sendPacket(sm);
			}
			else
			{
				sm = new SystemMessage(SystemMessage.S1_CREATED_S2_AFTER_RECEIVING_S3_ADENA);
				sm.addString(crafter.getName());
				sm.addItemName(product.itemId);
				sm.addNumber(price);
				employer.sendPacket(sm);

				sm = new SystemMessage(SystemMessage.S2_HAS_BEEN_CREATED_FOR_S1_AFTER_THE_PAYMENT_OF_S3_ADENA_IS_RECEIVED);
				sm.addString(employer.getName());
				sm.addItemName(product.itemId);
				sm.addNumber(price);
				crafter.sendPacket(sm);
			}

			if(!itemTemplate.isStackable() && product.quantity > 1)
				for(int i = 0; i < product.quantity; i++)
					employer.addItem("Craft", product.itemId, 1, crafter, true);
			else
				employer.addItem("Craft", product.itemId, product.quantity, crafter, true);
		}

		crafter.sendPacket(new StatusUpdate(crafter.getObjectId()).addAttribute(StatusUpdate.CUR_MP, (int) crafter.getCurrentMp()));
		employer.sendChanges();
		employer.sendPacket(new StatusUpdate(crafter.getObjectId()).addAttribute(StatusUpdate.CUR_LOAD, crafter.getCurrentLoad()));
		employer.sendPacket(new RecipeShopItemInfo(crafter.getObjectId(), recipeListId, success ? 1 : 0));
	}
}
