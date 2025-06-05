import { useState,  useContext} from 'react';
import { LogOut, ChevronDown } from 'lucide-react';
import LoggedinInfo from './LoggedinInfo';
import BarOption from './BarOption';
import { AdminContext } from '../../context/AdminContext';
import { adminLogout } from '../../services/services';
import { useNavigate } from 'react-router-dom';
import { clearAllTokens, getToken} from '../../services/tokenStorage';
export default function TopBarProfile(){
    const {admin, setAdmin , setRole ,setIsLoggedIn} = useContext(AdminContext);
    const [isOpen, setIsOpen] = useState(false);
    const navigate = useNavigate();
    const toggleDropdown = () => {
        setIsOpen(!isOpen);
    };
    const handleOnClick = async ()=>{
      console.log(getToken("refreshToken"));
      const response = await adminLogout(admin);
      if(response.status === 200){
        setAdmin(null);
        setRole(null);
        setIsLoggedIn(false);
        clearAllTokens();
        console.log("Logout successful");
        
    }
    else{
      console.log(response.data.message);
    }
    navigate("/login")
  }
    return (
          <div className="bg-white overflow-visible w-[350px] relative">

            <div className="flex items-center p-4 cursor-pointer gap-[120px]" onClick={toggleDropdown}>
                <LoggedinInfo title="Super Admin" name="Amin Khaled"></LoggedinInfo>
                <ChevronDown className={`text-gray-400 transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`} size={24} />
            </div>
            
            <div className={`absolute top-full left-0 right-0 z-10 bg-white shadow-md transition-all duration-300 ease-in-out ${isOpen ? 'opacity-100 visible' : 'opacity-0 invisible'}`}>
              <div className="flex flex-col gap-2 p-4 border-t border-gray-100">
                <button onClick={handleOnClick}><BarOption icon={<LogOut />} title="Logout" /></button>
              </div>
            </div>

          </div>
      );
}