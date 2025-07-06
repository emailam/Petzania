import { UserContext } from "../../context/userContext";
import { useContext } from "react";
import DetailsRow from "../../components/DetailsRow";
import { User, CircleUser, FileText, Mail, Shield, CheckCircle, Calendar, Heart, Users, UserPlus } from "lucide-react";

export default function UserDetails() {
  const { user } = useContext(UserContext);
  
  // Define fields to display with their icons and labels
  const userFields = [
    { key: "name", label: "Full Name", icon: <User className="h-5 w-5 text-purple-500" /> },
    { key: "username", label: "Username", icon: <User className="h-5 w-5 text-blue-500" /> },
    { key: "bio", label: "Bio", icon: <FileText className="h-5 w-5 text-green-500" /> },
    { key: "email", label: "Email", icon: <Mail className="h-5 w-5 text-yellow-500" /> },
    { key: "userId", label: "User ID", icon: <CircleUser className="h-5 w-5 text-gray-500" /> },
    { key: "userRoles", label: "Roles", icon: <Shield className="h-5 w-5 text-indigo-500" /> },
    { key: "active", label: "Active Status", icon: <CheckCircle className="h-5 w-5 text-emerald-500" /> },
    { key: "loginTimes", label: "Login Times", icon: <Calendar className="h-5 w-5 text-teal-500" /> },
    { key: "friendsCount", label: "Friends", icon: <Heart className="h-5 w-5 text-red-400" /> },
    { key: "followersCount", label: "Followers", icon: <Users className="h-5 w-5 text-pink-500" /> },
    { key: "followingCount", label: "Following", icon: <UserPlus className="h-5 w-5 text-red-500" /> },
  ];
  
  return (
    <div className="flex flex-col w-full bg-gray-100">
      {user ? (
        <>
          <div className="bg-primary p-4 rounded-t-lg shadow-md text-left">
            <h1 className="text-xl font-bold text-gray-200">User Information</h1>
            <p className="text-gray-300 text-sm mt-1">Detailed profile information for {user.username}</p>
          </div>
          <div className="bg-white rounded-b-lg shadow-md overflow-hidden w-full">
            <div className="flex flex-col gap-4 p-4 w-full">
              {userFields.map((field) => (
                user[field.key] !== undefined && (
                  <DetailsRow 
                    key={field.key}
                    icon={field.icon}
                    label={field.label}
                    value={user[field.key]}
                  />
                )
              ))}
            </div>
          </div>
        </>
      ) : (
        <div className="bg-white p-6 rounded-lg shadow-md text-left">
          <p className="text-gray-600">User information not available</p>
        </div>
      )}
    </div>
  );
}