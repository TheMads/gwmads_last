package ru.l2gw.gameserver.model.variation;

/**
 * @author: rage
 * @date: 18.10.11 21:54
 */
public class VariationFee
{
	public final int fee_item_id;
	public final long fee_count, cancel_fee;

	public VariationFee(int itemId, long count, long cancel)
	{
		fee_item_id = itemId;
		fee_count = count;
		cancel_fee = cancel;
	}
}
