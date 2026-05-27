package userinfo;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {UserProfile.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserProfileDao userProfileDao();
}