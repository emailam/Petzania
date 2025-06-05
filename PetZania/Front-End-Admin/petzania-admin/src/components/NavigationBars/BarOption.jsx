import { Link } from "react-router-dom";

export default function BarOption({ icon, title ,href }) {
    return (
        <Link className="flex items-center gap-4 hover:bg-gray-200 rounded-md cursor-pointer" to ={href}>
            {icon}
            <span className="text-lg ">{title}</span>
        </Link>
    )
}