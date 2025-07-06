import BarOption from "./BarOption"
import { PawPrint , Users , Repeat2 , Group  } from 'lucide-react';
export default function ReportOptions(){
    return(
        <div className='flex flex-col gap-4'>
            <h1 className='text-3xl font-semibold border-b border-b-gray-300 pb-[15px]'>Tickets</h1>
            <div className="flex flex-col gap-4">
                <BarOption icon={<Users />} title="Users" href="/users" />
                <BarOption icon={<Repeat2 />} title="Posts" href="/reports" />
                <BarOption icon={<Group />} title="Groups" href="/groups" />
                <BarOption icon={<PawPrint />} title="Adoption & Breeding" href="/pets" />
            </div>
        </div>
    )
}