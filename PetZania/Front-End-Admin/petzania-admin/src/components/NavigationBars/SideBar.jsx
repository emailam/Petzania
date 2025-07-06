import ActionOptions from "./ActionOptions";
import ReportOptions from "./ReportOptions";
export default function SideBar(){
    return(
        <div className="flex flex-col w-1/6 h-screen  p-6 gap-12 border-r border-gray-300 shadow-lg"> 
            <h1 className="text-5xl font-bold">PetZania</h1>
            <ReportOptions />
            <ActionOptions />
        </div>
    )
}