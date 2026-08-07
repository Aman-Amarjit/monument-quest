export interface SocialFeedItem {
  id: string;
  userId: string;
  userName: string;
  userRank: string;
  monumentName: string;
  locationName: string;
  imageUrl?: string;
  caption: string;
  postType: string;
  likesCount: number;
  isLiked: boolean;
  commentsCount: number;
  timestamp: number;
}

export class FeedService {
  private static postsList: SocialFeedItem[] = [
    {
      id: 'p1',
      userId: 'u1',
      userName: 'Aarav Patnaik',
      userRank: 'Temple City Historian',
      monumentName: 'Lingaraj Temple',
      locationName: 'Old Town, Bhubaneswar',
      imageUrl: 'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800&auto=format&fit=crop',
      caption: 'Early morning visit to Lingaraj Temple! The 55m Deula spire lit up in warm morning light is an absolute masterpiece of 11th-century Kalinga architecture. 🛕✨',
      postType: 'CHECKIN',
      likesCount: 84,
      isLiked: true,
      commentsCount: 12,
      timestamp: Date.now() - 1000 * 60 * 20
    },
    {
      id: 'p2',
      userId: 'u2',
      userName: 'Priya Mohanty',
      userRank: 'Master Explorer',
      monumentName: 'Mukteshvara Temple',
      locationName: 'Kedargouri, Bhubaneswar',
      imageUrl: 'https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=800&auto=format&fit=crop',
      caption: 'The iconic carved Torana archway of Mukteshvara is stunning! Left an AR Time Capsule near the sacred Marichi Kunda tank. 🔮📜',
      postType: 'TIME_CAPSULE',
      likesCount: 62,
      isLiked: false,
      commentsCount: 7,
      timestamp: Date.now() - 1000 * 60 * 110
    },
    {
      id: 'p3',
      userId: 'u3',
      userName: 'Subhashree Das',
      userRank: 'First Discoverer',
      monumentName: 'Dhauli Shanti Stupa',
      locationName: 'Dhauli Hills, Bhubaneswar',
      imageUrl: 'https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?q=80&w=800&auto=format&fit=crop',
      caption: 'Stood on Dhauli Hills where Emperor Ashoka renounced war after the Kalinga War in 261 BC. The peace pagoda overlooks the Daya River. 🕊️🌿',
      postType: 'DISCOVERY',
      likesCount: 115,
      isLiked: true,
      commentsCount: 18,
      timestamp: Date.now() - 1000 * 3600 * 5
    }
  ];

  public static async getFeed(filter: string = 'GLOBAL'): Promise<SocialFeedItem[]> {
    if (filter === 'NEARBY') {
      return this.postsList.filter(p => p.locationName.includes('Bhubaneswar'));
    }
    return this.postsList;
  }

  public static async createPost(
    userId: string,
    userName: string,
    monumentName: string,
    caption: string,
    postType: string = 'CHECKIN',
    imageUrl?: string
  ): Promise<SocialFeedItem> {
    const newPost: SocialFeedItem = {
      id: 'sp_' + Date.now(),
      userId,
      userName: userName || 'Adventurer',
      userRank: 'Bhubaneswar Explorer',
      monumentName,
      locationName: 'Bhubaneswar, Odisha',
      imageUrl,
      caption,
      postType,
      likesCount: 1,
      isLiked: true,
      commentsCount: 0,
      timestamp: Date.now()
    };

    this.postsList.unshift(newPost);
    return newPost;
  }

  public static async toggleLike(postId: string): Promise<{ likesCount: number; isLiked: boolean }> {
    const post = this.postsList.find(p => p.id === postId);
    if (!post) throw new Error('Post not found');

    post.isLiked = !post.isLiked;
    post.likesCount += post.isLiked ? 1 : -1;
    return { likesCount: post.likesCount, isLiked: post.isLiked };
  }
}
