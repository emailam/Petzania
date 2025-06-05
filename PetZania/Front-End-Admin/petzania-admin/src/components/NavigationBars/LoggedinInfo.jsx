import { useContext } from "react";
import { AdminContext } from "../../context/AdminContext";
import logo from "../../assets/image.png";
export default function LoggedinInfo() {
    const { admin, role } = useContext(AdminContext);
    return (
        <div className="flex items-center rounded-md">
            <img src={logo} alt="Logo" className="w-12 h-auto rounded-full mr-2" />
            <div>
                <span className="text-lg ">{admin}</span>
                <p className="text-sm text-gray-500">{role}</p>
            </div>
        </div>
    )
}