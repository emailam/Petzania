import { Users , FileText , ShieldUser  } from 'lucide-react';
import BarOption from './BarOption';
import { AdminContext } from '../../context/AdminContext';
import { useContext } from 'react';
export default function ActionOptions(){
    const { role } = useContext(AdminContext);
    return(
        <div className='flex flex-col gap-4'>
            <h1 className='text-3xl font-semibold border-b border-b-gray-300 pb-[15px]'>Actions</h1>
            
            <div className="flex flex-col gap-4">
                <BarOption icon={<Users />} title="Users" href="/app/users" />
                {role ==="SUPER_ADMIN" && <BarOption icon={<ShieldUser />} title="Admin" href="/app/admins" />} 
                <BarOption icon={<FileText />} title="Documents" href="/reports" />
            </div>
        </div>
    )
}