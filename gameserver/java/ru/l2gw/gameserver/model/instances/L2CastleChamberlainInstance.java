package ru.l2gw.gameserver.model.instances;

import javolution.text.TextBuilder;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.TradeController;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.reinforce.DoorReinforce;
import ru.l2gw.gameserver.model.entity.siege.reinforce.TrapReinforce;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.util.Files;
import ru.l2gw.util.Util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.StringTokenizer;

public class L2CastleChamberlainInstance extends L2ClanBaseManagerInstance
{
	private static org.apache.commons.logging.Log _log = LogFactory.getLog(L2CastleChamberlainInstance.class.getName());

	private static int Cond_All_False = 0;
	private static int Cond_Busy_Because_Of_Siege = 1;
	private static int Cond_Clan = 2;
	private static int Cond_Clan_wPrivs = 3;
	private static int Cond_Owner = 4;
	private final static String path = "data/html/castle/chamberlain/";
	private int _preDay;
	private int _preHour;
	private int _trapId;
	private int _trapLevel;
	private int _drId = 0;
	private int _drLevel = 0;

	public L2CastleChamberlainInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(player.isActionsDisabled() || player.isSitting() || player.getLastNpc().getDistance(player) > 300)
			return;

		String actualCommand = command;

		player.sendActionFailed();

		int condition = validateCondition(player);
		if(condition <= Cond_All_False)
			return;

		if(condition == Cond_Busy_Because_Of_Siege)
		{
		}
		else if(condition == Cond_Owner || condition == Cond_Clan_wPrivs)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken(); // Get actual command
			String val = "";
			if(st.countTokens() >= 1)
				val = st.nextToken();

			if(command.equalsIgnoreCase("banish_foreigner"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_DISMISS))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!val.equals(""))
				{
					getBuilding(-1).banishForeigner(); // Move non-clan members off castle area
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "chamberlain-afterbanish.htm");
					player.sendPacket(html);
					return;
				}

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-banish.htm");
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("list_siege_clans"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}
				getBuilding(2).getSiege().listRegisterClan(player); // List current register clan
			}
			else if(command.equalsIgnoreCase("CastleFunctions"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_SET_FUNCTIONS))
				{
					showForbiddenMessage(player);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-castlefunc.htm");
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("UseCastleFunctions"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_USE_FUNCTIONS))
				{
					showForbiddenMessage(player);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-usecastlefunc.htm");
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("ManageTreasure"))
			{
				if(!player.isClanLeader())
				{
					showForbiddenMessage(player);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-castlevault.htm");
				html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
				html.replace("%CollectedShops%", String.valueOf(getCastle().getCollectedShops()));
				html.replace("%CollectedSeed%", String.valueOf(getCastle().getCollectedSeed()));
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("TakeTreasure"))
			{
				if(!player.isClanLeader())
				{
					showForbiddenMessage(player);
					return;
				}
				if(!val.equals(""))
				{
					int treasure = 0;
					try
					{
						treasure = Integer.parseInt(val);
					}
					catch(NumberFormatException e)
					{
					}

					if(getCastle().getTreasury() < treasure)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "chamberlain-havenottreasure.htm");
						html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
						html.replace("%Requested%", String.valueOf(treasure));
						player.sendPacket(html);
						return;
					}
					if(treasure > 0)
					{
						getCastle().addToTreasuryNoTax(-treasure, false, false, "WITHDRAW");
						player.addAdena("Treashure", treasure, this, true);
					}
				}

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-castlevault.htm");
				html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
				html.replace("%CollectedShops%", String.valueOf(getCastle().getCollectedShops()));
				html.replace("%CollectedSeed%", String.valueOf(getCastle().getCollectedSeed()));
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("PutTreasure"))
			{
				if(!val.equals(""))
				{
					int treasure = Integer.parseInt(val);
					if(treasure > player.getAdena())
					{
						player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
						return;
					}
					if(treasure > 0 && player.reduceAdena("Treshure", treasure, this, true))
					{
						getCastle().addToTreasuryNoTax(treasure, false, false, "DEPOSIT");
					}
				}

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-castlevault.htm");
				html.replace("%Treasure%", String.valueOf(getCastle().getTreasury()));
				html.replace("%CollectedShops%", String.valueOf(getCastle().getCollectedShops()));
				html.replace("%CollectedSeed%", String.valueOf(getCastle().getCollectedSeed()));
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("manor"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANOR_ADMIN))
				{
					showForbiddenMessage(player);
					return;
				}
				String filename = "";
				if(CastleManorManager.getInstance().isDisabled())
					filename = "data/html/npcdefault.htm";
				else
				{
					int cmd = Integer.parseInt(val);
					switch(cmd)
					{
						case 0:
							filename = path + "manor/manor.htm";
							break;
						// TODO: correct in html's to 1
						case 4:
							filename = path + "manor/manor_help00" + st.nextToken() + ".htm";
							break;
						default:
							filename = path + "chamberlain-no.htm";
							break;
					}
				}

				if(filename.length() > 0)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(filename);
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%npcname%", getName());
					player.sendPacket(html);
				}
			}
			else if(command.startsWith("manor_menu_select"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANOR_ADMIN))
				{
					showForbiddenMessage(player);
					return;
				}
				// input string format:
				// manor_menu_select?ask=X&state=Y&time=X
				if(CastleManorManager.getInstance().isUnderMaintenance())
				{
					player.sendPacket(Msg.ActionFail);
					player.sendPacket(new SystemMessage(SystemMessage.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE));
					return;
				}

				String params = command.substring(command.indexOf("?") + 1);
				StringTokenizer str = new StringTokenizer(params, "&");
				int ask = Integer.parseInt(str.nextToken().split("=")[1]);
				int state = Integer.parseInt(str.nextToken().split("=")[1]);
				int time = Integer.parseInt(str.nextToken().split("=")[1]);

				int castleId;
				if(state == -1) // info for current manor
					castleId = getBuilding(2).getId();
				else
					// info for requested manor
					castleId = state;

				switch(ask)
				{ // Main action
					case 3: // Current seeds (Manor info)
						if(time == 1 && !ResidenceManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
							player.sendPacket(new ExShowSeedInfo(castleId, null));
						else
							player.sendPacket(new ExShowSeedInfo(castleId, ResidenceManager.getInstance().getCastleById(castleId).getSeedProduction(time)));
						break;
					case 4: // Current crops (Manor info)
						if(time == 1 && !ResidenceManager.getInstance().getCastleById(castleId).isNextPeriodApproved())
							player.sendPacket(new ExShowCropInfo(castleId, null));
						else
							player.sendPacket(new ExShowCropInfo(castleId, ResidenceManager.getInstance().getCastleById(castleId).getCropProcure(time)));
						break;
					case 5: // Basic info (Manor info)
						player.sendPacket(new ExShowManorDefaultInfo());
						break;
					case 7: // Edit seed setup
						if(getCastle().isNextPeriodApproved())
							player.sendPacket(new SystemMessage(SystemMessage.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
						else
							player.sendPacket(new ExShowSeedSetting(getBuilding(2).getId()));
						break;
					case 8: // Edit crop setup
						if(getCastle().isNextPeriodApproved())
							player.sendPacket(new SystemMessage(SystemMessage.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_AM_AND_8_PM));
						else
							player.sendPacket(new ExShowCropSetting(getBuilding(2).getId()));
						break;
				}
			}
			else if(command.equalsIgnoreCase("operate_door")) // door control
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!val.equals(""))
				{
					boolean open = Integer.parseInt(val) == 1;
					while(st.hasMoreTokens())
						getBuilding(2).openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
				}

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "" + getTemplate().npcId + "-d.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("tax_set")) // tax rates control
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_TAXES))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!val.equals(""))
				{
					// По умолчанию налог не более 15%
					Integer maxTax = 15;
					// Если печатью SEAL_STRIFE владеют DUSK то налог можно выставлять не более 5%
					if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
						maxTax = 5;
						// Если печатью SEAL_STRIFE владеют DAWN то налог можно выставлять не более 25%
					else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
						maxTax = 25;

					if(Integer.parseInt(val) < 0 || Integer.parseInt(val) > maxTax)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "chamberlain-hightax.htm");
						html.replace("%maxTax%", String.valueOf(maxTax));
						html.replace("%CurrentTax%", String.valueOf(getCastle().getTaxPercent()));
						player.sendPacket(html);
						return;
					}
					getCastle().setTaxPercent(player, Integer.parseInt(val));
				}

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-settax.htm");
				html.replace("%CurrentTax%", String.valueOf(getCastle().getTaxPercent()));
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("upgrade_castle"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-upgrades.htm");
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("reinforce_walls"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
				}

				TextBuilder txt = new TextBuilder("");
				for(DoorReinforce rg : getCastle().getDoorReinforce())
				{
					txt.append("<a action=\"bypass -h npc_")
							.append(String.valueOf(getObjectId()))
							.append("_preReinforce ")
							.append(String.valueOf(rg.getId())).append("\">").append(rg.getName()).append("</a><br>");
				}

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

				html.setFile(path + "chamber-walls.htm");
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				html.replace("%reinforces%", txt.toString());
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("preReinforce"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
				}


				try
				{
					_drId = _drLevel = 0;
					if(!val.equals(""))
					{
						_drId = Integer.parseInt(val);

						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamber-rlevel.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);

					}
				}
				catch(Exception e)
				{
				}
			}
			else if(command.equalsIgnoreCase("doReinforce"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
				}

				if(_drId != 0 && !val.equals(""))
				{
					_drLevel = Integer.parseInt(val);
					if(_drLevel > 3) _drLevel = 3;
					if(_drLevel < 1) _drLevel = 1;
					DoorReinforce dr = (DoorReinforce) getCastle().getReinforceById(_drId);

					if(dr.getLevel() >= _drLevel)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamber-lvlerr.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						html.replace("%doorlevel%", String.valueOf((int) dr.getMultByLevel(dr.getLevel()) * 100));
						player.sendPacket(html);
						return;
					}

					int price = dr.getPrice(_drLevel);
					if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
						price *= 2;
					else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
						price /= 2;

					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setFile(path + "chamber-slevel.htm");
					html.replace("%objectId%", String.valueOf(getObjectId()));
					html.replace("%gate_price%", String.valueOf(price));
					player.sendPacket(html);
				}
			}
			else if(command.equalsIgnoreCase("reinforce"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}
				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
					return;
				}

				if(_drId != 0 && _drLevel != 0)
				{
					DoorReinforce dr = (DoorReinforce) getCastle().getReinforceById(_drId);
					int price = dr.getPrice(_drLevel);

					if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
						price *= 2;
					else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
						price /= 2;

					L2Clan clan = player.getClan();
					if(clan.getWarehouse().getItemByItemId(57) != null && clan.getWarehouse().getItemByItemId(57).getCount() >= price)
					{
						clan.getWarehouse().destroyItemByItemId("ReinforceLvl" + _drLevel, 57, price, player, this);
						dr.setLevel(_drLevel);
						dr.store();
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamber-rok.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
					else
					{
						player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamberlain-upgrades.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
				}
			}

			else if(command.equalsIgnoreCase("siege_change")) // siege day set
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
				}
				else
				{
					if(getBuilding(2).getSiege().getChangeTimeEnd().getTimeInMillis() < Calendar.getInstance().getTimeInMillis())
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(path + "siegetime1.htm");
						sendHtmlMessage(player, html);
					}
					else if(getBuilding(2).getSiege().isChangeTimeOver())
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setFile(path + "siegetime2.htm");
						sendHtmlMessage(player, html);
					}
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "siegetime3.htm");
						html.replace("%time%", String.valueOf(getBuilding(2).getSiege().getSiegeDate().getTime()));
						sendHtmlMessage(player, html);
					}
				}
			}
			else if(command.equalsIgnoreCase("siege_time_set")) // set preDay
			{
				int act = Integer.parseInt(val);
				switch(act)
				{
					case 0: // List available time
						HashMap<Integer, String> tpls = Util.parseTemplate(Files.read(path + "siegetime6.htm", player, false));
						String tpl = tpls.get(0);
						String links = "";
						for(int hour : Config.SIEGE_HOUR_LIST)
							links += tpls.get(1).replace("<?hour?>", String.valueOf(hour));

						tpl = tpl.replace("<?LINKS?>", links);
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setHtml(tpl);
						sendHtmlMessage(player, html);
						break;
					case 1: // Set time
						int hour = Integer.parseInt(st.nextToken());
						if(Config.SIEGE_HOUR_LIST.contains(hour))
						{
							getBuilding(2).getSiege().getSiegeDate().set(Calendar.HOUR_OF_DAY, hour);
							getBuilding(2).getSiege().updateSiegeTime();
						}
						html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "siegetime8.htm");
						html.replace("%time%", String.valueOf(getBuilding(2).getSiege().getSiegeDate().getTime()));
						sendHtmlMessage(player, html);
						break;
					default:
						html = new NpcHtmlMessage(1);
						html.setFile(path + "siegetime2.htm");
						sendHtmlMessage(player, html);
						break;
				}
			}
			else if(command.equalsIgnoreCase("showTrap"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}

				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
					return;
				}

				NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(path + "chamber-showtrap.htm");
				TextBuilder txt = new TextBuilder();
				for(TrapReinforce ct : getCastle().getTrapReinforce())
					txt.append("<a action=\"bypass -h npc_" + getObjectId() + "_selectTrap " + ct.getId() + "\">" + ct.getName() + "</a><br>");

				html.replace("%trapdevices%", txt.toString());
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("selectTrap"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}

				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
					return;
				}

				try
				{
					_trapId = _trapLevel = 0;
					if(!val.equals(""))
					{
						_trapId = Integer.parseInt(val);
						if(getCastle().getReinforceById(_trapId) == null)
							return;

						TextBuilder txt = new TextBuilder();
						for(int i = 0; i < ((TrapReinforce) getCastle().getReinforceById(_trapId)).getMaxLevel(); i++)
						{
							txt.append("<a action=\"bypass -h npc_" + getObjectId() + "_selectTrapLvl " + (i + 1) + "\">Level " + (i + 1) + "</a><br>");
						}

						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamber-traplvl.htm");
						html.replace("%traplevels%", txt.toString());
						player.sendPacket(html);
					}
				}
				catch(Exception e)
				{
				}
			}
			else if(command.equalsIgnoreCase("selectTrapLvl"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}

				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
					return;
				}

				try
				{
					if(!val.equals(""))
					{
						_trapLevel = Integer.parseInt(val);
						if(_trapLevel < 1)
							_trapLevel = 1;

						TrapReinforce tr = (TrapReinforce) getCastle().getReinforceById(_trapId);

						if(tr != null && _trapLevel > tr.getMaxLevel())
							_trapLevel = tr.getMaxLevel();

						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						if(_trapLevel <= tr.getLevel())
						{
							html.setFile(path + "chamber-traplvlerr.htm");
							html.replace("%dmglevel%", String.valueOf(tr.getLevel()));
							player.sendPacket(html);
							return;
						}

						html.setFile(path + "chamber-trapconfirm.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						int price = tr.getPrice(_trapLevel);

						if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
							price *= 2;
						else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
							price /= 2;

						html.replace("%dmgzone_price%", String.valueOf(price));
						player.sendPacket(html);
					}
				}
				catch(Exception e)
				{
				}
			}
			else if(command.equalsIgnoreCase("deployTrap"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE))
				{
					showForbiddenMessage(player);
					return;
				}

				if(!SevenSigns.getInstance().isSealValidationPeriod())
				{
					showNoValidMessage(player);
					return;
				}

				if(_trapId != 0 && _trapLevel != 0)
				{
					TrapReinforce tr = (TrapReinforce) getCastle().getReinforceById(_trapId);
					int price = tr.getPrice(_trapLevel);

					if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
						price *= 2;
					else if(SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
						price /= 2;

					L2Clan clan = player.getClan();
					if(clan.getWarehouse().getItemByItemId(57) != null && clan.getWarehouse().getItemByItemId(57).getCount() >= price)
					{
						clan.getWarehouse().destroyItemByItemId("DeployTrapLvl" + _trapLevel, 57, price, player, this);
						tr.setLevel(_trapLevel);
						tr.store();
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamber-trapok.htm");
						player.sendPacket(html);
					}
					else
					{
						player.sendPacket(new SystemMessage(SystemMessage.THERE_IS_NOT_ENOUGH_ADENA_IN_THE_CLAN_HALL_WAREHOUSE));
						NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setFile(path + "chamberlain-upgrades.htm");
						html.replace("%objectId%", String.valueOf(getObjectId()));
						player.sendPacket(html);
					}
				}
			}
			else if(command.equalsIgnoreCase("report")) // Report page
			{
				if(!player.isClanLeader())
				{
					showForbiddenMessage(player);
					return;
				}
				String ssq_period;
				if(SevenSigns.getInstance().getCurrentPeriod() == 1)
					ssq_period = "Competition";
				else if(SevenSigns.getInstance().getCurrentPeriod() == 3)
					ssq_period = "Effective sealing";
				else
					ssq_period = "Ready";

				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain-report.htm");
				html.replace("%FeudName%", getBuilding(2).getName());
				html.replace("%CharClan%", player.getClan().getName());
				html.replace("%CharName%", player.getName());
				html.replace("%SSPeriod%", ssq_period);
				html.replace("%Avarice%", getSealOwner(1));
				html.replace("%Revelation%", getSealOwner(2));
				html.replace("%Strife%", getSealOwner(3));
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("items")) // Main page for items
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_USE_FUNCTIONS))
				{
					showForbiddenMessage(player);
					return;
				}
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "" + getTemplate().getNpcId() + "-items.htm");
				html.replace("%npcname%", getName());
				player.sendPacket(html);
			}
			else if(command.equalsIgnoreCase("Crown")) // Give Crown to Castle Owner
			{
				if(!player.isClanLeader())
				{
					showForbiddenMessage(player);
					return;
				}
				if(player.getInventory().getItemByItemId(6841) == null)
				{
					player.addItem("CastleCrown", 6841, 1, this, true);

					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "gavecrown.htm");
					html.replace("%CharName%", String.valueOf(player.getName()));
					html.replace("%FeudName%", getBuilding(2).getName());
					player.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "alreadyhavecrown.htm");
					player.sendPacket(html);
				}
			}
			else if(command.equalsIgnoreCase("Buy"))
			{
				if(!isHaveRigths(player, L2Clan.CP_CS_USE_FUNCTIONS))
				{
					showForbiddenMessage(player);
					return;
				}
				if(val.equals(""))
					return;
				showBuyWindow(player, Integer.parseInt(val));
			}
			else if(command.equalsIgnoreCase("default"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "chamberlain.htm");
				player.sendPacket(html);
			}
			if(command.equalsIgnoreCase("TWRegistration"))
			{
				player.setLastNpc(this);
				player.sendPacket(new ExShowDominionRegistry(player, TerritoryWarManager.getTerritoryById(_territoryId)));
			}
			else
				super.onBypassFeedback(player, actualCommand);
		}
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String filename = path + "chamberlain-notlord.htm";
		int condition = validateCondition(player);
		if(condition > Cond_All_False)
			if(condition == Cond_Busy_Because_Of_Siege)
				filename = path + "chamberlain-busy.htm";
			else if(condition == Cond_Owner || condition == Cond_Clan_wPrivs) // Clan owns castle
				filename = path + "chamberlain.htm";
		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private void showForbiddenMessage(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(path + "chamberlain-forbidden.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcId%", String.valueOf(getNpcId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	private void showNoValidMessage(L2Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(path + "chamber-novalid.htm");
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

	@Override
	protected int validateCondition(L2Player player)
	{
		SiegeUnit castle = getBuilding(2);
		if(castle.getId() > 0)
			if(player.getClanId() != 0)
				if(castle.getSiege().isInProgress() || TerritoryWarManager.getWar().isInProgress())
					return Cond_Busy_Because_Of_Siege; // Busy because of siege
				else if(castle.getOwnerId() == player.getClanId())
				{
					if(player.isClanLeader()) // Leader of clan
						return Cond_Owner;
					if(isHaveRigths(player, L2Clan.CP_CS_OPEN_DOOR) || // doors
							isHaveRigths(player, L2Clan.CP_CS_MANOR_ADMIN) || // manor
							isHaveRigths(player, L2Clan.CP_CS_MANAGE_SIEGE) || // siege
							isHaveRigths(player, L2Clan.CP_CS_USE_FUNCTIONS) || // funcs
							isHaveRigths(player, L2Clan.CP_CS_DISMISS) || // banish
							isHaveRigths(player, L2Clan.CP_CS_TAXES) || // tax
							isHaveRigths(player, L2Clan.CP_CS_MERCENARIES) || // merc
							isHaveRigths(player, L2Clan.CP_CS_SET_FUNCTIONS) //funcs
							)
						return Cond_Clan_wPrivs; // Есть какие либо замковые привилегии
					return Cond_Clan;
				}

		return Cond_All_False;
	}

	private String getSealOwner(int seal)
	{
		switch(SevenSigns.getInstance().getSealOwner(seal))
		{
			case SevenSigns.CABAL_DUSK:
				return "Evening";
			case SevenSigns.CABAL_DAWN:
				return "Dawn";
			default:
				return "None belongs";
		}
	}

	@Override
	public void showBuyWindow(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noShop", player))
			return;

		player.tempInvetoryDisable();
		if(Config.DEBUG)
			_log.debug("Showing buylist");
		NpcTradeList list = TradeController.getInstance().getSellList(val);
		if(list != null && list.getNpcId()  == getNpcId())
		{
			player.sendPacket(new ExBuyList(list, player));
			player.sendPacket(new ExSellRefundList(player));
		}
		else
			_log.warn(player.getName() + " wrong sell list: " + list + " id: " + val + " for npc: " + this + " player loc: " + player.getLoc());
	}

	@Override
	protected String getHtmlPath()
	{
		return "data/html/castle/chamberlain/base/";
	}

	@Override
	protected String getManagePath()
	{
		return "";
	}

	@Override
	protected boolean canSetFunctions(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_SET_FUNCTIONS) == L2Clan.CP_CS_SET_FUNCTIONS;
	}

	@Override
	protected boolean canUseFunctions(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_USE_FUNCTIONS) == L2Clan.CP_CS_USE_FUNCTIONS;
	}

	@Override
	protected boolean canUseDoors(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_OPEN_DOOR) == L2Clan.CP_CS_OPEN_DOOR;
	}

	@Override
	protected boolean canDismiss(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CS_DISMISS) == L2Clan.CP_CS_DISMISS;
	}
}