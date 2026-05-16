export default function ServiceCard({ icon: Icon, title, description, to }) {
  return (
    <a href={to}
      className="block bg-white rounded-2xl p-6 shadow-sm border border-gray-100
                 hover:shadow-md hover:border-primary-200 transition-all group">
      <div className="w-12 h-12 bg-primary-50 rounded-xl flex items-center
                      justify-center mb-4 group-hover:bg-primary-100 transition-colors">
        <Icon className="text-primary-600" size={24} />
      </div>
      <h3 className="font-semibold text-gray-900 mb-1">{title}</h3>
      <p className="text-sm text-gray-500">{description}</p>
    </a>
  )
}