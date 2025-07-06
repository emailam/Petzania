import SideBar from "./components/NavigationBars/SideBar";
import TopBar from "./components/NavigationBars/TopBar";
import DetailsRow from "./components/DetailsRow";
import LoginPage from "./Pages/LoginPage";
import api from "./services/api";
import UserDetails from "./Pages/UserPages/UserDetails";
import UserProfile from "./Pages/UserPages/UserProfile";
import AllUsers from "./Pages/UserPages/AllUsers";
import { Outlet } from "react-router-dom";
function App() {
 
  return (
    <div className="flex flex-col-2 gap-0">
              <SideBar/>
        
              <div className="flex flex-1 flex-col gap-4 bg-gray-100">
                <TopBar/>
                <div className="w-full px-4">
                  <Outlet/>
                </div>
              </div>         
    </div>
  
  )
}

export default App
