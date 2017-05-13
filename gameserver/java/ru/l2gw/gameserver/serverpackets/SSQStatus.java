package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalManager;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalParty;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.SevenSignsFestival;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.List;

/**
 * Seven Signs Record Update
 * <p/>
 * packet type id 0xf5
 * format:
 * <p/>
 * c cc	(Page Num = 1 -> 4, period)
 * <p/>
 * 1: [ddd cc dd ddd c ddd c]
 * 2: [hc [cd (dc (S))]
 * 3: [ccc (cccc)]
 * 4: [(cchh)]
 */
public class SSQStatus extends L2GameServerPacket
{
	private L2Player _player;
	private int _page, period;

	public SSQStatus(L2Player player, int recordPage)
	{
		_player = player;
		_page = recordPage;
		period = SevenSigns.getInstance().getCurrentPeriod();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xFB);

		writeC(_page);
		writeC(period); // current period?

		switch(_page)
		{
			case 1:
				// [ddd cc dd ddd c ddd c]
				writeD(SevenSigns.getInstance().getCurrentCycle());

				switch(period)
				{
					case SevenSigns.PERIOD_COMP_RECRUITING:
						writeD(1183);
						break;
					case SevenSigns.PERIOD_COMPETITION:
						writeD(1176);
						break;
					case SevenSigns.PERIOD_COMP_RESULTS:
						writeD(1184);
						break;
					case SevenSigns.PERIOD_SEAL_VALIDATION:
						writeD(1177);
						break;
				}

				switch(period)
				{
					case SevenSigns.PERIOD_COMP_RECRUITING:
					case SevenSigns.PERIOD_COMP_RESULTS:
						writeD(1287);
						break;
					case SevenSigns.PERIOD_COMPETITION:
					case SevenSigns.PERIOD_SEAL_VALIDATION:
						writeD(1286);
						break;
				}

				writeC(SevenSigns.getInstance().getPlayerCabal(_player));
				writeC(SevenSigns.getInstance().getPlayerSeal(_player));

				writeQ(SevenSigns.getInstance().getPlayerStoneContrib(_player)); // Seal Stones Turned-In
				writeQ(SevenSigns.getInstance().getPlayerAdenaCollect(_player)); // Ancient Adena to Collect

				long dawnStoneScore = SevenSigns.getInstance().getCurrentStoneScore(SevenSigns.CABAL_DAWN);
				long dawnFestivalScore = SevenSignsFestival.getInstance().getFestivalScore(SevenSigns.CABAL_DAWN);
				long dawnTotalScore = dawnStoneScore + dawnFestivalScore;

				long duskStoneScore = SevenSigns.getInstance().getCurrentStoneScore(SevenSigns.CABAL_DUSK);
				long duskFestivalScore = SevenSignsFestival.getInstance().getFestivalScore(SevenSigns.CABAL_DUSK);
				long duskTotalScore = duskStoneScore + duskFestivalScore;

				long totalStoneScore = duskStoneScore + dawnStoneScore;

				/*
				 * Scoring seems to be proportionate to a set base value, so base this on
				 * the maximum obtainable score from festivals, which is 500.
				 */
				long duskStoneScoreProp = 0;
				long dawnStoneScoreProp = 0;

				if(totalStoneScore > 0)
				{
					duskStoneScoreProp = duskStoneScore * 500 / totalStoneScore;
					dawnStoneScoreProp = 500 - duskStoneScoreProp;
				}

				long totalOverallScore = duskTotalScore + dawnTotalScore;

				int dawnPercent = 0;
				int duskPercent = 0;

				if((duskStoneScoreProp + duskFestivalScore + dawnStoneScoreProp + dawnFestivalScore) > 0)
				{
					duskPercent = (int) (110 * (duskStoneScoreProp + duskFestivalScore) / (duskStoneScoreProp + duskFestivalScore + dawnStoneScoreProp + dawnFestivalScore));
					dawnPercent = 110 - duskPercent;
				}
				if(Config.DEBUG)
				{
					_log.info("Dusk Stone Score: " + duskStoneScore + " - Dawn Stone Score: " + dawnStoneScore);
					_log.info("Dusk Festival Score: " + duskFestivalScore + " - Dawn Festival Score: " + dawnFestivalScore);
					_log.info("Dusk Score: " + duskTotalScore + " - Dawn Score: " + dawnTotalScore);
					_log.info("Overall Score: " + totalOverallScore);
					_log.info("");
					_log.info("Dusk Prop: " + duskStoneScoreProp + " - Dawn Prop: " + dawnStoneScoreProp);
					_log.info("Dusk %: " + duskPercent + " - Dawn %: " + dawnPercent);
				}

				/* DUSK */
				writeQ(duskStoneScoreProp); // Seal Stone Score
				writeQ(duskFestivalScore); // Festival Score
				writeQ(duskStoneScoreProp + duskFestivalScore); // Total Score

				writeC(duskPercent); // Dusk %

				/* DAWN */
				writeQ(dawnStoneScoreProp); // Seal Stone Score
				writeQ(dawnFestivalScore); // Festival Score
				writeQ(dawnStoneScoreProp + dawnFestivalScore); // Total Score

				writeC(dawnPercent); // Dawn %
				break;
			case 2:
				// c cc hc [cd (dc (S))]
				List<Integer> levels = FestivalManager.getInstance().getFestivalLevels();
				writeH(1);
				writeC(levels.size()); // Total number of festivals

				int c = 1;
				for(Integer level : levels)
				{
					writeC(c);
					writeD(FestivalManager.getInstance().getFestivalRewardPoints(level));

					FestivalParty duskParty = SevenSignsFestival.getInstance().getCurrentTopParty(FestivalManager.getInstance().getFestivalIdByCabalLevel(SevenSigns.CABAL_DUSK, level));
					FestivalParty dawnParty = SevenSignsFestival.getInstance().getCurrentTopParty(FestivalManager.getInstance().getFestivalIdByCabalLevel(SevenSigns.CABAL_DAWN, level));

					if(duskParty != null)
					{
						writeQ(duskParty.getScore());
						writeC(duskParty.getMembers().size());
						for(StatsSet member : duskParty.getMembers())
							writeS(member.getString("name"));
					}
					else
					{
						writeQ(0x00);
						writeC(0x00);
					}

					if(dawnParty != null)
					{
						writeQ(dawnParty.getScore());
						writeC(dawnParty.getMembers().size());
						for(StatsSet member : dawnParty.getMembers())
							writeS(member.getString("name"));
					}
					else
					{
						writeQ(0x00);
						writeC(0x00);
					}
					c++;
				}
				break;
			case 3:
				// c cc [ccc (cccc)]
				writeC(10); // Minimum limit for winning cabal to retain their seal
				writeC(35); // Minimum limit for winning cabal to claim a seal
				writeC(3); // Total number of seals

				int totalDawnProportion = 0;
				int totalDuskProportion = 0;

				for(int i = 1; i <= 3; i++)
				{
					totalDawnProportion += SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					totalDuskProportion += SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);
				}

				for(int i = 1; i <= 3; i++)
				{
					int dawnProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					int duskProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);

					writeC(i);
					writeC(SevenSigns.getInstance().getSealOwner(i));
					writeC(totalDuskProportion > 0 ? duskProportion * 100 / totalDuskProportion : 0);
					writeC(totalDawnProportion > 0 ? dawnProportion * 100 / totalDawnProportion : 0);
				}
				break;
			case 4:
				// c cc [(cchh)]

				writeC(SevenSigns.getInstance().getCabalWinner()); // Overall predicted winner
				writeC(3); // Total number of seals
				if(Config.DEBUG)
					_log.info("Overall Predicted Winner: " + SevenSigns.getInstance().getCabalWinner());
				int totalDawnSigned = 0;
				int totalDuskSigned = 0;

				for(int i = 1; i <= 3; i++)
				{
					totalDawnSigned += SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					totalDuskSigned += SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);
				}
				if(Config.DEBUG)
					_log.info("Total Signed: Dusk - " + totalDuskSigned + " Dawn - " + totalDawnSigned);
				for(int i = 1; i <= 3; i++)
				{
					if(Config.DEBUG)
						_log.info("Seal " + i);
					int dawnProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					int duskProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);

					if(Config.DEBUG)
						_log.info("Seal Proportions: Dusk - " + duskProportion + " Dawn - " + dawnProportion);
					
					int signed = 0;
					if(totalDuskSigned > 0 && SevenSigns.getInstance().getCabalWinner() == SevenSigns.CABAL_DUSK)
						signed = duskProportion * 100 / totalDuskSigned;
					else if(totalDawnSigned > 0 && SevenSigns.getInstance().getCabalWinner() == SevenSigns.CABAL_DAWN)
						signed = dawnProportion * 100 / totalDawnSigned;

					if(Config.DEBUG)
						_log.info("Signed for the seal: " + signed);

					int sealOwner = SevenSigns.getInstance().getSealOwner(i);

					if(Config.DEBUG)
						_log.info("Last owner: " + sealOwner);

					int limit = 35;
					if(sealOwner == SevenSigns.getInstance().getCabalWinner() && sealOwner != 0)
						limit = 10;
					int winner = SevenSigns.CABAL_NULL;
					if(signed >= limit)
						winner = SevenSigns.getInstance().getCabalWinner();

					if(Config.DEBUG)
					{
						_log.info("Limit: " + limit);
						_log.info("Winner: " + winner);
					}
					writeC(i);
					writeC(winner);

					/*
					 * 1289 = 10% or more voted for owned seal
					 * 1290 = 35% or more voted for non-owned seal
					 * 1291 = 10% or less voted for owned seal
					 * 1292 = 35% or less voted for non-owned seal
					 */
					if(limit == 10)
					{
						if(signed >= 10)
							writeH(1289);
						else
							writeH(1291);
					}
					else if(limit == 35)
					{
						if(signed >= 35)
							writeH(1290);
						else
							writeH(1292);
					}
					// Shows a short description of the seal status when not Seal Validation.
					if(SevenSigns.getInstance().isSealValidationPeriod())
						writeH(1);
					else
						writeH(0);
				}

				break;
		}
	}
}