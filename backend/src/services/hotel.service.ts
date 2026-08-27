export interface GeoLocation {
  latitude: DoubleRange | number;
  longitude: DoubleRange | number;
}

export class HotelService {
  private static partners = [
    {
      id: "h1",
      name: "Mayfair Lagoon Bhubaneswar",
      distance: "1.2 km",
      discount: "25% OFF Heritage Dining",
      perk: "Free Welcome Drink & 25% Off Dining",
      voucherCode: "HERITAGE25",
      imageUrl: "https://images.unsplash.com/photo-1566073771259-6a8506099945?q=80&w=800",
      latitude: 20.3015,
      longitude: 85.8246
    },
    {
      id: "h2",
      name: "Swosti Premium",
      distance: "2.8 km",
      discount: "20% OFF Spa & Stay",
      perk: "Complimentary Breakfast + Spa Discount",
      voucherCode: "EXPLORE20",
      imageUrl: "https://images.unsplash.com/photo-1582719508461-905c673771fd?q=80&w=800",
      latitude: 20.3085,
      longitude: 85.8340
    },
    {
      id: "h3",
      name: "Trident Bhubaneswar",
      distance: "3.5 km",
      discount: "30% OFF Luxury Stay",
      perk: "30% OFF Room Rate for Explorers",
      voucherCode: "TRIDENT30",
      imageUrl: "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?q=80&w=800",
      latitude: 20.2925,
      longitude: 85.8270
    }
  ];

  static nearby(_location: GeoLocation) {
    return this.partners;
  }

  static claim(hotelId: String) {
    const target = this.partners.find((h) => h.id === hotelId) || this.partners[0];
    return {
      success: true,
      voucherCode: target.voucherCode,
      message: `Voucher ${target.voucherCode} claimed for ${target.name}!`
    };
  }
}
