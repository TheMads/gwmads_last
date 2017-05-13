package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.Warehouse.WarehouseType;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

public class L2ClanBaseManagerInstance extends L2NpcInstance
{
	protected static Log _log = LogFactory.getLog(L2ClanBaseManagerInstance.class.getName());

	/**
	 * @param objectId
	 * @param template
	 */
	public L2ClanBaseManagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		player.sendPacket(Msg.ActionFail);
		int condition = validateCondition(player);
		if(condition <= Cond_All_False)
			return;
		else if(condition > Cond_Busy_Because_Of_Siege)
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			String actualCommand = st.nextToken(); // Get actual command
			String val = "";
			if(st.countTokens() >= 1)
				val = st.nextToken();

			String path = getHtmlPath();
			String manage_path = path + getManagePath();

			if(actualCommand.equalsIgnoreCase("banish"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

				html.setFile(path + "Banish.htm");
				sendHtmlMessage(player, html);
			}
			else if(actualCommand.equalsIgnoreCase("banish_foreigner"))
			{
				if(!canDismiss(player))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}
				
				getBuilding(-1).banishForeigner();
				NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
				html.setFile(path + "afterbanish.htm");
				sendHtmlMessage(player, html);
				return;
			}
			else if(actualCommand.equalsIgnoreCase("manage_vault"))
			{
				if(val.equalsIgnoreCase("deposit"))
					showDepositWindowClan(player);
				else if(val.equalsIgnoreCase("withdraw"))
				{
					int value = Integer.valueOf(st.nextToken());
					if(value == 9)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "clan.htm");
						html.replace("%npcname%", getName());
						player.sendPacket(html);
					}
					else
						showWithdrawWindowClan(player, value);
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

					html.setFile(path + "vault.htm");
					SiegeUnit unit = ResidenceManager.getInstance().getResidenceByOwner(player.getClanId(), true);
					if(unit != null)
					{
						html.replace("%lease%", String.valueOf(unit.getLease()));
						if(unit.isClanHall)
							html.replace("%paid%", format.format(((ClanHall) unit).getPaidUntil()));
					}
					else
					{
						html.replace("%lease%", "Unknow");
						html.replace("%paid%", "Unknow");
					}
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("door"))
			{
				if(canUseDoors(player))
				{
					if(val.equalsIgnoreCase("open"))
					{
						for(L2DoorInstance door : getBuilding(-1).getDoors())
							getBuilding(-1).openCloseDoor(player, door.getDoorId(), true);

						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "door-open.htm");
						sendHtmlMessage(player, html);
					}
					else if(val.equalsIgnoreCase("close"))
					{
						for(L2DoorInstance door : getBuilding(-1).getDoors())
							getBuilding(-1).openCloseDoor(player, door.getDoorId(), false);

						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "door-close.htm");
						sendHtmlMessage(player, html);
					}
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "door.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}
			}
			else if(actualCommand.equalsIgnoreCase("functions"))
			{
				if(val.equalsIgnoreCase("tele"))
				{
					if(!canUseFunctions(player))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "notauthorized.htm");
						sendHtmlMessage(player, html);
						return;
					}

					if(isFunctionDisabled(SiegeUnit.FUNC_TELEPORT))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "functions-disabled.htm");
						sendHtmlMessage(player, html);
						return;
					}

					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "tele" + getBuilding(-1).getLocation() + getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT).getLvl() + ".htm");
					sendHtmlMessage(player, html);
				}
				else if(val.equalsIgnoreCase("item_creation"))
				{
					if(!canUseFunctions(player))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "notauthorized.htm");
						sendHtmlMessage(player, html);
						return;
					}

					if(isFunctionDisabled(SiegeUnit.FUNC_ITEM_CREATE))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "functions-disabled.htm");
						sendHtmlMessage(player, html);
						return;
					}

					showBuyWindow(player, getBuyListId(getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE).getLvl()));
				}
				else if(val.equalsIgnoreCase("support"))
				{
					if(!canUseFunctions(player))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "notauthorized.htm");
						sendHtmlMessage(player, html);
						return;
					}

					if(isFunctionDisabled(SiegeUnit.FUNC_SUPPORT))
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "functions-disabled.htm");
						sendHtmlMessage(player, html);
						return;
					}

					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(manage_path + "support" + getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT).getLvl() + ".htm");
					html.replace("%mp%", String.valueOf(Math.round(getCurrentMp())));
					sendHtmlMessage(player, html);
				}
				else if(val.equalsIgnoreCase("back"))
					showChatWindow(player, -1);
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "functions.htm");
					html.replace("%xp_regen%", getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP) != null ? String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLvl()) : "0");
					html.replace("%hp_regen%", getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP) != null ? String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP).getLvl()) : "0");
					html.replace("%mp_regen%", getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP) != null ? String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP).getLvl()) : "0");
					sendHtmlMessage(player, html);
				}
			}
			else if(actualCommand.equalsIgnoreCase("manage"))
			{
				if(canSetFunctions(player))
				{
					if(val.equalsIgnoreCase("recovery"))
					{
						if(st.countTokens() >= 1)
						{
							val = st.nextToken();
							if(val.equalsIgnoreCase("hp"))
							{
								if(st.countTokens() >= 1)
								{
									val = st.nextToken();
									int percent = Integer.valueOf(val);

									int fee;
									if(Config.DEBUG)
										_log.warn("Hp editing invoked");
									switch(percent)
									{
										case 20:
											fee = Config.CH_HPREG1_FEE;
											break;
										case 40:
											fee = Config.CH_HPREG2_FEE;
											break;
										case 80:
											fee = Config.CH_HPREG3_FEE;
											break;
										case 100:
											fee = Config.CH_HPREG4_FEE;
											break;
										case 120:
											fee = Config.CH_HPREG5_FEE;
											break;
										case 140:
											fee = Config.CH_HPREG6_FEE;
											break;
										case 160:
											fee = Config.CH_HPREG7_FEE;
											break;
										case 180:
											fee = Config.CH_HPREG8_FEE;
											break;
										case 200:
											fee = Config.CH_HPREG9_FEE;
											break;
										case 240:
											fee = Config.CH_HPREG10_FEE;
											break;
										case 260:
											fee = Config.CH_HPREG11_FEE;
											break;
										default:
											fee = Config.CH_HPREG12_FEE;
											break;
									}

									if(st.countTokens() >= 1)
									{
										if(percent == 0)
										{
											getBuilding(-1).removeFunctions(SiegeUnit.FUNC_RESTORE_HP);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-cancel_confirmed.htm");
											sendHtmlMessage(player, html);
										}
										else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP).getLvl() == percent)
										{
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-used.htm");
											html.replace("%val%", val + "%");
											sendHtmlMessage(player, html);
										}
										else
										{
											if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
											{
												getBuilding(-1).updateFunctions(SiegeUnit.FUNC_RESTORE_HP, percent, fee, Config.CH_HPREG_FEE_RATIO);
												NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
												html.setFile(path + "functions-apply_confirmed.htm");
												sendHtmlMessage(player, html);
											}
										}
										broadCastClanHallInfo();
									}
									else
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

										if(percent == 0)
											html.setFile(path + "functions-cancel.htm");
										else
										{
											html.setFile(path + "functions-apply.htm");
											html.replace("%name%", "HP Recovery");
											html.replace("%cost%", String.valueOf(fee));
											html.replace("%use%", val + "%");
										}

										html.replace("%apply%", "recovery hp " + val + " confirm");
										sendHtmlMessage(player, html);
									}
								}
							}
							else if(val.equalsIgnoreCase("mp"))
							{
								if(st.countTokens() >= 1)
								{
									val = st.nextToken();
									int percent = Integer.valueOf(val);

									int fee;
									if(Config.DEBUG)
										_log.warn("Mp editing invoked");
									switch(percent)
									{
										case 5:
											fee = Config.CH_MPREG1_FEE;
											break;
										case 10:
											fee = Config.CH_MPREG2_FEE;
											break;
										case 15:
											fee = Config.CH_MPREG3_FEE;
											break;
										case 25:
											fee = Config.CH_MPREG4_FEE;
											break;
										case 30:
											fee = Config.CH_MPREG5_FEE;
											break;
										default:
											fee = Config.CH_MPREG6_FEE;
											break;
									}

									if(st.countTokens() >= 1)
									{
										if(percent == 0)
										{
											getBuilding(-1).removeFunctions(SiegeUnit.FUNC_RESTORE_MP);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-cancel_confirmed.htm");
											sendHtmlMessage(player, html);
										}
										else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP).getLvl() == percent)
										{
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-used.htm");
											html.replace("%val%", val + "%");
											sendHtmlMessage(player, html);
										}
										else
										{
											if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
											{
												getBuilding(-1).updateFunctions(SiegeUnit.FUNC_RESTORE_MP, percent, fee, Config.CH_MPREG_FEE_RATIO);
												NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
												html.setFile(path + "functions-apply_confirmed.htm");
												sendHtmlMessage(player, html);
											}
										}
										broadCastClanHallInfo();
									}
									else
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

										if(percent == 0)
											html.setFile(path + "functions-cancel.htm");
										else
										{
											html.setFile(path + "functions-apply.htm");
											html.replace("%name%", "MP Recovery");
											html.replace("%cost%", String.valueOf(fee));
											html.replace("%use%", val + "%");
										}

										html.replace("%apply%", "recovery mp " + val + " confirm");
										sendHtmlMessage(player, html);
									}
								}
							}
							else if(val.equalsIgnoreCase("exp"))
								if(st.countTokens() >= 1)
								{
									val = st.nextToken();
									int percent = Integer.valueOf(val);
									int fee;
									if(Config.DEBUG)
										_log.warn("Exp editing invoked");
									switch(percent)
									{
										case 5:
											fee = Config.CH_EXPREG1_FEE;
											break;
										case 10:
											fee = Config.CH_EXPREG2_FEE;
											break;
										case 15:
											fee = Config.CH_EXPREG3_FEE;
											break;
										case 25:
											fee = Config.CH_EXPREG4_FEE;
											break;
										case 30:
											fee = Config.CH_EXPREG5_FEE;
											break;
										case 35:
											fee = Config.CH_EXPREG6_FEE;
											break;
										case 40:
											fee = Config.CH_EXPREG7_FEE;
											break;
										default:
											fee = Config.CH_EXPREG8_FEE;
											break;
									}

									if(st.countTokens() >= 1)
									{
										if(percent == 0)
										{
											getBuilding(-1).removeFunctions(SiegeUnit.FUNC_RESTORE_EXP);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-cancel_confirmed.htm");
											sendHtmlMessage(player, html);
										}
										else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLvl() == percent)
										{
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-used.htm");
											html.replace("%val%", val + "%");
											sendHtmlMessage(player, html);
										}
										else
										{
											if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
											{
												getBuilding(-1).updateFunctions(SiegeUnit.FUNC_RESTORE_EXP, percent, fee, Config.CH_EXPREG_FEE_RATIO);
												NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
												html.setFile(path + "functions-apply_confirmed.htm");
												sendHtmlMessage(player, html);
											}
										}
										broadCastClanHallInfo();
									}
									else
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

										if(percent == 0)
											html.setFile(path + "functions-cancel.htm");
										else
										{
											html.setFile(path + "functions-apply.htm");
											html.replace("%name%", "Exp Recovery");
											html.replace("%cost%", String.valueOf(fee));
											html.replace("%use%", val + "%");
										}

										html.replace("%apply%", "recovery exp " + val + " confirm");
										sendHtmlMessage(player, html);
									}
								}
						}
						else
						{
							NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
							html.setFile(manage_path + "edit_recovery.htm");
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP) != null)
							{
								html.replace("%hp%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP).getLvl()) + "%");
								html.replace("%hpPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP).getLease()));
								html.replace("%hpDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_HP).getNextPayTime()));
							}
							else
							{
								html.replace("%hp%", "0");
								html.replace("%hpPrice%", "0");
								html.replace("%hpDate%", "0");
							}
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP) != null)
							{
								html.replace("%exp%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLvl()) + "%");
								html.replace("%expPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP).getLease()));
								html.replace("%expDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_EXP).getNextPayTime()));
							}
							else
							{
								html.replace("%exp%", "0");
								html.replace("%expPrice%", "0");
								html.replace("%expDate%", "0");
							}
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP) != null)
							{
								html.replace("%mp%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP).getLvl()) + "%");
								html.replace("%mpPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP).getLease()));
								html.replace("%mpDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_RESTORE_MP).getNextPayTime()));
							}
							else
							{
								html.replace("%mp%", "0");
								html.replace("%mpPrice%", "0");
								html.replace("%mpDate%", "0");
							}
							sendHtmlMessage(player, html);
						}
					}
					else if(val.equalsIgnoreCase("other"))
					{
						if(st.countTokens() >= 1)
						{
							val = st.nextToken();
							if(val.equalsIgnoreCase("item"))
							{
								val = st.nextToken();
								int lvl = Integer.valueOf(val);
								int fee = 30000;
								switch(lvl)
								{
									case 1:
										fee = Config.CH_ITEM1_FEE;
										break;
									case 2:
										fee = Config.CH_ITEM2_FEE;
										break;
									case 3:
										fee = Config.CH_ITEM3_FEE;
										break;
								}

								if(st.countTokens() >= 1)
								{
									if(Config.DEBUG)
										_log.warn("Item editing invoked");

									if(lvl == 0)
									{
										getBuilding(-1).removeFunctions(SiegeUnit.FUNC_ITEM_CREATE);
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
										html.setFile(path + "functions-cancel_confirmed.htm");
										sendHtmlMessage(player, html);
									}
									else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE).getLvl() == lvl)
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
										html.setFile(path + "functions-used.htm");
										html.replace("%val%", val + " level");
										sendHtmlMessage(player, html);
									}
									else
									{
										if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
										{
											getBuilding(-1).updateFunctions(SiegeUnit.FUNC_ITEM_CREATE, lvl, fee, Config.CH_ITEM_FEE_RATIO);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-apply_confirmed.htm");
											sendHtmlMessage(player, html);
										}
									}
									broadCastClanHallInfo();
								}
								else
								{
									NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

									if(lvl == 0)
										html.setFile(path + "functions-cancel.htm");
									else
									{
										html.setFile(path + "functions-apply.htm");
										html.replace("%name%", "Item Creation");
										html.replace("%cost%", String.valueOf(fee));
										html.replace("%use%", val + " level");
									}

									html.replace("%apply%", "other item " + val + " confirm");
									sendHtmlMessage(player, html);
								}
							}
							else if(val.equalsIgnoreCase("tele"))
							{
								if(st.countTokens() >= 1)
								{
									val = st.nextToken();
									int lvl = Integer.valueOf(val);
									int fee;
									if(Config.DEBUG)
										_log.warn("Tele editing invoked");
									switch(lvl)
									{
										case 1:
											fee = Config.CH_TELE1_FEE;
											break;
										default:
											fee = Config.CH_TELE2_FEE;
											break;
									}

									if(st.countTokens() >= 1)
									{
										if(lvl == 0)
										{
											getBuilding(-1).removeFunctions(SiegeUnit.FUNC_TELEPORT);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-cancel_confirmed.htm");
											sendHtmlMessage(player, html);
										}
										else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT).getLvl() == lvl)
										{
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-used.htm");
											html.replace("%val%", val + " level");
											sendHtmlMessage(player, html);
										}
										else
										{
											if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
											{
												getBuilding(-1).updateFunctions(SiegeUnit.FUNC_TELEPORT, lvl, fee, Config.CH_TELE_FEE_RATIO);
												NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
												html.setFile(path + "functions-apply_confirmed.htm");
												sendHtmlMessage(player, html);
											}
										}
										broadCastClanHallInfo();
									}
									else
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

										if(lvl == 0)
											html.setFile(path + "functions-cancel.htm");
										else
										{
											html.setFile(path + "functions-apply.htm");
											html.replace("%name%", "Teleport");
											html.replace("%cost%", String.valueOf(fee));
											html.replace("%use%", val + " level");
										}

										html.replace("%apply%", "other tele " + val + " confirm");
										sendHtmlMessage(player, html);
									}
								}
							}
							else if(val.equalsIgnoreCase("support"))
								if(st.countTokens() >= 1)
								{
									val = st.nextToken();
									int lvl = Integer.valueOf(val);
									int fee;
									if(Config.DEBUG)
										_log.warn("Support editing invoked");
									switch(lvl)
									{
										case 1:
											fee = Config.CH_SUPPORT1_FEE;
											break;
										case 2:
											fee = Config.CH_SUPPORT2_FEE;
											break;
										case 3:
											fee = Config.CH_SUPPORT3_FEE;
											break;
										case 4:
											fee = Config.CH_SUPPORT4_FEE;
											break;
										case 5:
											fee = Config.CH_SUPPORT5_FEE;
											break;
										case 7:
											fee = Config.CH_SUPPORT7_FEE;
											break;
										default:
											fee = Config.CH_SUPPORT8_FEE;
											break;
									}

									if(st.countTokens() >= 1)
									{
										if(lvl == 0)
										{
											getBuilding(-1).removeFunctions(SiegeUnit.FUNC_SUPPORT);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-cancel_confirmed.htm");
											sendHtmlMessage(player, html);
										}
										else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT).getLvl() == lvl)
										{
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-used.htm");
											html.replace("%val%", val + " level");
											sendHtmlMessage(player, html);
										}
										else
										{
											if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
											{
												getBuilding(-1).updateFunctions(SiegeUnit.FUNC_SUPPORT, lvl, fee, Config.CH_SUPPORT_FEE_RATIO);
												NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
												html.setFile(path + "functions-apply_confirmed.htm");
												sendHtmlMessage(player, html);
											}
										}
										broadCastClanHallInfo();
									}
									else
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

										if(lvl == 0)
											html.setFile(path + "functions-cancel.htm");
										else
										{
											html.setFile(path + "functions-apply.htm");
											html.replace("%name%", "Assist Magic");
											html.replace("%cost%", String.valueOf(fee));
											html.replace("%use%", val + " level");
										}

										html.replace("%apply%", "other support " + val + " confirm");
										sendHtmlMessage(player, html);
									}
								}
						}
						else
						{
							NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
							html.setFile(manage_path + "edit_other.htm");
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT) != null)
							{
								html.replace("%tele%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT).getLvl()));
								html.replace("%telePrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT).getLease()));
								html.replace("%teleDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_TELEPORT).getNextPayTime()));
							}
							else
							{
								html.replace("%tele%", "0");
								html.replace("%telePrice%", "0");
								html.replace("%teleDate%", "0");
							}
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT) != null)
							{
								html.replace("%support%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT).getLvl()));
								html.replace("%supportPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT).getLease()));
								html.replace("%supportDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_SUPPORT).getNextPayTime()));
							}
							else
							{
								html.replace("%support%", "0");
								html.replace("%supportPrice%", "0");
								html.replace("%supportDate%", "0");
							}
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE) != null)
							{
								html.replace("%item%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE).getLvl()));
								html.replace("%itemPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE).getLease()));
								html.replace("%itemDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_ITEM_CREATE).getNextPayTime()));
							}
							else
							{
								html.replace("%item%", "0");
								html.replace("%itemPrice%", "0");
								html.replace("%itemDate%", "0");
							}
							sendHtmlMessage(player, html);
						}
					}
					else if(val.equalsIgnoreCase("deco"))
					{
						if(st.countTokens() >= 1)
						{
							val = st.nextToken();
							if(val.equalsIgnoreCase("platform"))
							{
								val = st.nextToken();
								int lvl = Integer.valueOf(val);
								int fee;
								if(Config.DEBUG)
									_log.warn("Front Platform editing invoked");
								switch(lvl)
								{
									case 1:
										fee = Config.CH_PLATFORM1_FEE;
										break;
									default:
										fee = Config.CH_PLATFORM2_FEE;
										break;
								}

								if(st.countTokens() >= 1)
								{
									if(lvl == 0)
									{
										getBuilding(-1).removeFunctions(SiegeUnit.FUNC_PLATFORM);
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
										html.setFile(path + "functions-cancel_confirmed.htm");
										sendHtmlMessage(player, html);
									}
									else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_PLATFORM) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_PLATFORM).getLvl() == lvl)
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
										html.setFile(path + "functions-used.htm");
										html.replace("%val%", val + " Stage");
										sendHtmlMessage(player, html);
									}
									else
									{
										if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
										{
											getBuilding(-1).updateFunctions(SiegeUnit.FUNC_PLATFORM, lvl, fee, Config.CH_PLATFORM_FEE_RATIO);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-apply_confirmed.htm");
											sendHtmlMessage(player, html);
										}
									}
									broadCastClanHallInfo();
								}
								else
								{
									NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

									if(lvl == 0)
										html.setFile(path + "functions-cancel.htm");
									else
									{
										html.setFile(path + "functions-apply.htm");
										html.replace("%name%", "Front Platform");
										html.replace("%cost%", String.valueOf(fee));
										html.replace("%use%", val + " Stage");
									}

									html.replace("%apply%", "deco platform " + val + " confirm");
									sendHtmlMessage(player, html);
								}
							}
							else if(val.equalsIgnoreCase("curtain"))
								if(st.countTokens() >= 1)
								{
									val = st.nextToken();
									int lvl = Integer.valueOf(val);
									int fee;
									if(Config.DEBUG)
										_log.warn("Curtains editing invoked");
									switch(lvl)
									{
										case 1:
											fee = Config.CH_CURTAIN1_FEE;
											break;
										default:
											fee = Config.CH_CURTAIN2_FEE;
											break;
									}

									if(st.countTokens() >= 1)
									{
										if(lvl == 0)
										{
											getBuilding(-1).removeFunctions(SiegeUnit.FUNC_CURTAIN);
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-cancel_confirmed.htm");
											sendHtmlMessage(player, html);
										}
										else if(getBuilding(-1).getFunction(SiegeUnit.FUNC_CURTAIN) != null && getBuilding(-1).getFunction(SiegeUnit.FUNC_CURTAIN).getLvl() == lvl)
										{
											NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
											html.setFile(path + "functions-used.htm");
											html.replace("%val%", val + " Stage");
											sendHtmlMessage(player, html);
										}
										else
										{
											if(player.destroyItemByItemId("CHFunctionFee", 57, fee, this, true))
											{
												getBuilding(-1).updateFunctions(SiegeUnit.FUNC_CURTAIN, lvl, fee, Config.CH_CURTAIN_FEE_RATIO);
												NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
												html.setFile(path + "functions-apply_confirmed.htm");
												sendHtmlMessage(player, html);
											}
										}
										broadCastClanHallInfo();
									}
									else
									{
										NpcHtmlMessage html = new NpcHtmlMessage(_objectId);

										if(lvl == 0)
											html.setFile(path + "functions-cancel.htm");
										else
										{
											html.setFile(path + "functions-apply.htm");
											html.replace("%name%", "Curtains");
											html.replace("%cost%", String.valueOf(fee));
											html.replace("%use%", val + " Stage");
										}

										html.replace("%apply%", "deco curtain " + val + " confirm");
										sendHtmlMessage(player, html);
									}
								}
						}
						else
						{
							NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
							html.setFile(manage_path + "edit_deco.htm");
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_CURTAIN) != null)
							{
								html.replace("%curtain%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_CURTAIN).getLvl()));
								html.replace("%curtainPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_CURTAIN).getLease()));
								html.replace("%curtainDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_CURTAIN).getNextPayTime()));
							}
							else
							{
								html.replace("%curtain%", "0");
								html.replace("%curtainPrice%", "0");
								html.replace("%curtainDate%", "0");
							}
							if(getBuilding(-1).getFunction(SiegeUnit.FUNC_PLATFORM) != null)
							{
								html.replace("%platform%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_PLATFORM).getLvl()));
								html.replace("%platformPrice%", String.valueOf(getBuilding(-1).getFunction(SiegeUnit.FUNC_PLATFORM).getLease()));
								html.replace("%platformDate%", format.format(getBuilding(-1).getFunction(SiegeUnit.FUNC_PLATFORM).getNextPayTime()));
							}
							else
							{
								html.replace("%platform%", "0");
								html.replace("%platformPrice%", "0");
								html.replace("%platformDate%", "0");
							}
							sendHtmlMessage(player, html);
						}
					}
					else if(val.equalsIgnoreCase("back"))
						showChatWindow(player, -1);
					else
					{
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "manage.htm");
						sendHtmlMessage(player, html);
					}
				}
				else
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "notauthorized.htm");
					sendHtmlMessage(player, html);
				}
				return;
			}
			else if(actualCommand.equalsIgnoreCase("support"))
			{
				if(!canUseFunctions(player))
				{
					NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
					html.setFile(path + "notauthorized.htm");
					sendHtmlMessage(player, html);
					return;
				}

				setTarget(player);
				L2Skill skill;
				if(val.equals(""))
					return;

				try
				{
					int skill_id = Integer.parseInt(val);
					try
					{
						int skill_lvl = 0;
						if(st.countTokens() >= 1)
							skill_lvl = Integer.parseInt(st.nextToken());
						skill = SkillTable.getInstance().getInfo(skill_id, skill_lvl);
						if(skill.getMpConsume() > getCurrentMp())
						{
							NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
							html.setFile(path + "NeedCoolTime.htm");
							html.replace("%mp%", String.valueOf(Math.round(getCurrentMp())));
							sendHtmlMessage(player, html);
							return;
						}
						altUseSkill(skill, player, null);
						NpcHtmlMessage html = new NpcHtmlMessage(_objectId);
						html.setFile(path + "AfterSupport.htm");
						html.replace("%mp%", String.valueOf(Math.round(getCurrentMp())));
						sendHtmlMessage(player, html);
						return;
					}
					catch(Exception e)
					{
						player.sendMessage("Invalid skill level!");
					}
				}
				catch(Exception e)
				{
					player.sendMessage("Invalid skill!");
				}
				return;
			}
		}
		super.onBypassFeedback(player, command);
	}

	protected void sendHtmlMessage(L2Player player, NpcHtmlMessage html)
	{
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}

	@Override
	public void showChatWindow(L2Player player, int val)
	{
		String path = getHtmlPath();

		String filename = path + "manager-no.htm";
		int condition = validateCondition(player);
		if(condition == Cond_Owner) // Clan owns CH
		{
			if(getBuilding(-1).isClanHall && !getBuilding(-1).isPaid() && val == 0)
			{
				filename = path + "agitcostfail.htm"; // Owner message window
				NpcHtmlMessage html = new NpcHtmlMessage(player, this, filename, val);
				html.replace("%CostFailDayLeft%", String.valueOf((((ClanHall) getBuilding(-1)).getPaidUntil() + 604800000 - System.currentTimeMillis()) / 60000 / 60 / 24));
				player.sendPacket(html);
				return;
			}
			filename = path + "manager.htm"; // Owner message window
		}
		player.setLastNpc(this);

		player.sendPacket(new NpcHtmlMessage(player, this, filename, val));
	}

	private void showDepositWindowClan(L2Player player)
	{
		if(AdminTemplateManager.checkBoolean("noClanWarehouse", player))
			return;

		if(player.getClanId() == 0)
		{
			player.sendActionFailed();
			return;
		}

		if(player.getClan().getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		player.setUsingWarehouseType(WarehouseType.CLAN);
		player.tempInvetoryDisable();

		if(Config.DEBUG)
			_log.debug("Showing items to deposit - clan");

		if(!(player.isClanLeader() || Config.ALT_ALLOW_OTHERS_WITHDRAW_FROM_CLAN_WAREHOUSE && isHaveRigths(player, L2Clan.CP_CL_VIEW_WAREHOUSE)))
			player.sendPacket(Msg.ITEMS_LEFT_AT_THE_CLAN_HALL_WAREHOUSE_CAN_ONLY_BE_RETRIEVED_BY_THE_CLAN_LEADER_DO_YOU_WANT_TO_CONTINUE);

		player.sendPacket(new WareHouseDepositList(player, WarehouseType.CLAN));
	}

	private void showWithdrawWindowClan(L2Player player, int val)
	{
		if(AdminTemplateManager.checkBoolean("noClanWarehouse", player))
			return;

		if(player.getClanId() == 0)
		{
			player.sendActionFailed();
			return;
		}

		L2Clan _clan = player.getClan();

		if(_clan.getLevel() == 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE));
			player.sendActionFailed();
			return;
		}

		if(isHaveRigths(player, L2Clan.CP_CL_VIEW_WAREHOUSE))
		{
			player.setUsingWarehouseType(WarehouseType.CLAN);
			player.tempInvetoryDisable();
			player.sendPacket(new WareHouseWithdrawList(player, WarehouseType.CLAN));
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE));
			player.sendActionFailed();
		}
	}

	protected int validateCondition(final L2Player player)
	{
//		if(player.isGM())
//			return Cond_Owner;
		SiegeUnit unit = getBuilding(-1);
		if(unit.isClanHall && unit.getSiegeZone() != null || !unit.isClanHall)
		{
			if(unit.getSiege().isInProgress())
				return Cond_Busy_Because_Of_Siege;
		}
		if(player.getClanId() != 0)
			if(unit.getOwnerId() == player.getClanId())
				return Cond_Owner;
		return Cond_All_False;
	}

	protected String getHtmlPath()
	{
		return "data/html/clanhall/";
	}

	protected String getManagePath()
	{
		return "manage/" + (getAIParams() != null ? getAIParams().getString("clanhall_grade", "1") : "1") + "/"; 
	}

	public void sendDecoInfo(L2Player player)
	{
		SiegeUnit su = getBuilding(-1);
		if(su != null)
			player.sendPacket(new AgitDecoInfo(su));
	}

	public void broadCastClanHallInfo()
	{
		SiegeUnit su = getBuilding(-1);
		if(su != null)
			broadcastPacket(new AgitDecoInfo(su));
	}

	protected boolean canSetFunctions(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CH_SET_FUNCTIONS) == L2Clan.CP_CH_SET_FUNCTIONS;
	}

	protected boolean canUseFunctions(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CH_USE_FUNCTIONS) == L2Clan.CP_CH_USE_FUNCTIONS;
	}

	protected boolean canUseDoors(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CH_OPEN_DOOR) == L2Clan.CP_CH_OPEN_DOOR;
	}

	protected boolean canDismiss(L2Player player)
	{
		return (player.getClanPrivileges() & L2Clan.CP_CH_DISMISS) == L2Clan.CP_CH_DISMISS;
	}

	protected int getBuyListId(int lvl)
	{
		return Integer.parseInt("" + getNpcId() + lvl);
	}

	protected boolean isFunctionDisabled(int function)
	{
		return getBuilding(-1).getFunction(function) == null || getBuilding(-1).getFunction(function).getLvl() == 0;
	}
}