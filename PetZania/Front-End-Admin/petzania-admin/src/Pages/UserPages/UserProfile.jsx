import { UserContext } from "../../context/userContext";
import { useEffect, useContext, useState } from "react";
import { getUserById } from "../../services/services";
import UserDetails from "./UserDetails";
import UserPets from "./UserPets";
import UserPosts from "./UserPosts";
import UserIssues from "./UserIssues";
import { useParams } from "react-router-dom";

export default function UserProfile() {
    const { user, setUser } = useContext(UserContext);
    const [activeComponent, setActiveComponent] = useState("details");
    const { userId } = useParams();
    
    const fetchUser = async () => {
        try {
            const response = await getUserById(userId);
            setUser(response.data);
            console.log("User data fetched:", response.data);
        } catch (error) {
            console.error("Error fetching user data:", error);
        }
    };

    useEffect(() => {
        fetchUser();
    }, []);

    // Render the component based on activeComponent state
    const renderActiveComponent = () => {
        switch (activeComponent) {
            case "details":
                return <UserDetails />;
            case "pets":
                return <UserPets />;
            case "posts":
                return <UserPosts />;
            case "issues":
                return <UserIssues />;
            default:
                return null;
        }
    };

    return (
        <div className="flex flex-col w-full bg-gray-100">
            <div className="flex items-center justify-evenly p-6 w-full text-primary">
                <div>
                    <img
                        src={user?.profilePictureURL}
                        alt="User Profile"
                        className="w-[128px] h-[128px] rounded-full border-2 border-gray-300"
                    />
                </div>
                {/* Clickable navigation items */}
                <div
                    className={`text-xl font-bold cursor-pointer ${activeComponent === "details" ? "text-primary" : "text-gray-500"}`}
                    onClick={() => setActiveComponent("details")}
                >
                    User Details
                </div>
                <div
                    className={`text-xl font-bold cursor-pointer ${activeComponent === "pets" ? "text-primary" : "text-gray-500"}`}
                    onClick={() => setActiveComponent("pets")}
                >
                    User Pets
                </div>
                <div
                    className={`text-xl font-bold cursor-pointer ${activeComponent === "posts" ? "text-primary" : "text-gray-500"}`}
                    onClick={() => setActiveComponent("posts")}
                >
                    User Posts
                </div>
                <div
                    className={`text-xl font-bold cursor-pointer ${activeComponent === "issues" ? "text-primary" : "text-gray-500"}`}
                    onClick={() => setActiveComponent("issues")}
                >
                    User Issues
                </div>
            </div>
            {/* Parent container where the selected component is mounted - now full width */}
            <div className="w-full">
                {renderActiveComponent()}
            </div>
        </div>
    );
}